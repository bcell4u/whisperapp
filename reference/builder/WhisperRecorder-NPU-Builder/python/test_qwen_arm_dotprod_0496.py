"""v0.4.96 tests: bounded first-run Q8 and ARM64 dotprod native-only build.

No Android, network, real QNN or model weights required. All fake phone paths
are in the exact isolated existing test ownership scope.
"""
import argparse
import json
from pathlib import Path
import tempfile
import types
import unittest
from unittest.mock import patch
import zipfile

import numpy as np
import qwen_bridge_full_benchmark as runmod

ROOT=Path(__file__).resolve().parent.parent
DOT=runmod.DOTPROD_PROTOCOL


def synthetic_native_metric(count, *, cpu=True):
    rows=[]
    for i in range(count):
        rows.append('WR_BRIDGE_MULTI_METRIC='+json.dumps({
            'index':i,'prefill_ms':120,'generation_ms':90,'generation_steps':8,
            'eos_reached':True,'rss_peak_kb':880000,
            'token_hash_fnv64':'0c123456789abcde'}))
    rows.append('WR_BRIDGE_MULTI_SUMMARY='+json.dumps({
        'segments':count,'model_loads':1,'model_load_ms':340,
        'decoder_segments_wall_ms':count*215,'rss_peak_kb':880000,
        'mrope_parity_verified':False, **({'decode_threads':4,'batch_threads':4,
        'process_cpu_ms':count*700} if cpu else {})}))
    return '\n'.join(rows)+'\n'


class FakePhone:
    remote='/data/local/tmp/whisperrecorder-omi-v2-123456abcdef'
    staged=True
    def __init__(self,optimized=True,valid_info=True):
        self.optimized=optimized
        self.valid_info=valid_info
        self.commands=[]
        self.calls={'qwen_native_bridge_encoder':0}
        self.qnn_command_completed={'qwen_native_bridge_encoder':0}
        self.total_seconds={'qwen_native_bridge_encoder':0.0}
        self.clean_count=0
    def stage(self):
        return {'model':'SM-S942U'}
    def run(self,role,feed,shape,np,reference=None):
        self.calls[role]+=1
        self.qnn_command_completed[role]+=1
        self.total_seconds[role]+=.45
        return [np.zeros(runmod.FEATURE_SHAPE,dtype=np.float32)]
    def adb_call(self,cmd,timeout=None):
        self.commands.append(cmd)
        if '--probe-info' in cmd:
            data={
                'multi_protocol':runmod.MULTI_PROTOCOL.decode(),
                'cpu_tune_protocol':runmod.CPU_TUNE_PROTOCOL.decode(),
            }
            if self.optimized and self.valid_info:
                data['cpu_target']='arm64_dotprod'
                data['cpu_target_protocol']=DOT.decode()
            return types.SimpleNamespace(stdout='WR_BRIDGE_PROBE_INFO='+json.dumps(data,separators=(',',':'))+'\n',returncode=0)
        if '--multi' in cmd:
            n=2 if 'private-canary-list.txt' in str(cmd) else 3
            return types.SimpleNamespace(stdout=synthetic_native_metric(n,cpu=n!=2),returncode=0)
        if 'dumpsys' in cmd:
            if 'battery' in cmd:
                return types.SimpleNamespace(stdout='level: 85\ntemperature: 270\n',returncode=0)
            return types.SimpleNamespace(stdout='Thermal Status: 0',returncode=0)
        return types.SimpleNamespace(stdout='',returncode=0)
    def cleanup(self):
        self.clean_count+=1
        return 'remote path absence verified.'

class DotprodCandidateTests(unittest.TestCase):
    def _phone_session(self,optimized=True,valid_info=True,quant=False):
        with tempfile.TemporaryDirectory() as tmp:
            home=Path(tmp)
            results=home/'results';results.mkdir()
            cache=home/'cache'/'models';cache.mkdir(parents=True)
            (cache/runmod.MODEL_NAME).write_bytes(b'fixture')
            probe=home/'wr-qwen-gguf-bridge-probe'
            probe.write_bytes(runmod.MULTI_PROTOCOL+runmod.CPU_TUNE_PROTOCOL+(DOT if optimized else b''))
            args=argparse.Namespace(adb='adb',serial='PRIVATE_SERIAL',ffmpeg='ffmpeg',audio='private.wav',
                qnn_context='ctx',runtime_manifest='runtime',cache=str(home/'cache'),
                probe_archive=str(probe),results=str(results),max_seconds=36,
                threads=4,steps=96,full_trials=1,q8_fast_baseline=True,
                cpu_thread_sweep=False,dual_thread_sweep=False,temperature_match=False,
                quant_compare=quant,quant_q4_model=None,quant_q5_model=None,ack_provisional_accuracy=True)
            mock=FakePhone(optimized,valid_info)
            chunks=[(np.zeros((1,128,1200),dtype=np.float32),
                {'index':i,'start_s':12*i,'duration_s':12}) for i in range(3)]
            with patch.object(runmod,'verify_existing_accuracy',return_value={}), \
                 patch.object(runmod,'read_model',return_value={'sha256_verified':True}), \
                 patch.object(runmod,'validate_encoder_context',return_value={'bytes':100}), \
                 patch.object(runmod,'runtime_paths',return_value={}), \
                 patch.object(runmod,'extract_probe',return_value=probe), \
                 patch.object(runmod,'decode_bounded_pcm',return_value=b'\x00\x00'*(36*16000)), \
                 patch.object(runmod,'segment_pcm',return_value=chunks):
                code,report=runmod.run(args,hooks={'engine':lambda *a:mock})
            with zipfile.ZipFile(report) as z:
                self.assertEqual(set(z.namelist()),{'manifest.json','privacy.txt'})
                content=z.read('manifest.json').decode('utf-8')
                receipt=json.loads(content)
                self.assertNotIn('PRIVATE_SERIAL',content)
                self.assertNotIn('private.wav',content)
            return code,receipt,mock

    def test_optimized_single_trial_phone_simulation(self):
        code,r,phone=self._phone_session()
        self.assertEqual(code,0)
        self.assertEqual(phone.calls['qwen_native_bridge_encoder'],3)
        self.assertEqual(r['phone_all_files_cleanup'],'verified')
        self.assertEqual(phone.clean_count,1)
        self.assertEqual(r['native_cpu_target'],'arm64_dotprod_candidate')
        self.assertTrue(r['native_cpu_isa_optimization_phone_verified'])
        self.assertEqual(r['native_decoder']['process_cpu_ms'],2100)
        self.assertEqual(len(r['native_trials']),1)
        self.assertEqual(r['model_loads_for_session'],2)  # canary + single full run
        full_cmds=[c for c in phone.commands if '--multi' in c]
        self.assertEqual(len(full_cmds),2)
        self.assertEqual(full_cmds[-1][-2:],['--decode-threads','4'])
        self.assertFalse(r['calibrated_energy_measured'])
        self.assertFalse(r['mrope_parity_verified'])

    def test_legacy_cpu_probe_single_trial_remains_supported(self):
        code,r,phone=self._phone_session(optimized=False)
        self.assertEqual(code,0)
        self.assertEqual(r['native_cpu_target'],'baseline_arm64')
        self.assertFalse(r['native_cpu_isa_optimization_phone_verified'])
        self.assertEqual(phone.clean_count,1)

    def test_spoofed_optimized_marker_not_enough_without_phone_attestation(self):
        code,r,phone=self._phone_session(optimized=True,valid_info=False)
        self.assertEqual(code,2)
        self.assertEqual(r['error_stage'],'native_protocol_probe')
        self.assertEqual(r['error_code'],'ARM_DOTPROD_NATIVE_PROBE_INFO_OR_DEVICE_UNSUPPORTED')
        self.assertEqual(r['phone_all_files_cleanup'],'verified')
        self.assertEqual(phone.calls['qwen_native_bridge_encoder'],0)
        self.assertEqual(phone.clean_count,1)

    def test_invalid_mode_combination_stops_before_phone_stage(self):
        code,r,phone=self._phone_session(quant=True)
        self.assertEqual(code,2)
        self.assertEqual(r['error_code'],'Q8_FAST_BASELINE_MUST_BE_SINGLE_FOUR_THREAD_TRIAL')
        self.assertEqual(r['phone_all_files_cleanup'],'not_staged')
        self.assertEqual(phone.clean_count,0)

    def test_optional_workflow_and_source_are_in_sync(self):
        workflow=(ROOT/'.github/workflows/qwen-native-bridge-dotprod-probe.yml').read_text()
        for name,source in [
            ('CMakeLists.txt', ROOT/'native/qwen_bridge/CMakeLists.txt'),
            ('qwen_npu_llama_probe.cpp',ROOT/'native/qwen_bridge/qwen_npu_llama_probe.cpp')]:
            tag='WR_CMAKE_EOF' if name=='CMakeLists.txt' else 'WR_CPP_EOF'
            prefix=f"cat > native/qwen_bridge/{name} <<'{tag}'\n"
            body=workflow.split(prefix,1)[1].split('\n          '+tag,1)[0]
            embedded='\n'.join(line[10:] if line.startswith('          ') else line for line in body.splitlines())+'\n'
            self.assertEqual(source.read_text(),embedded)
        self.assertIn('-DWR_ENABLE_ARM_DOTPROD=ON',workflow)
        self.assertIn('name: qwen-android-arm64-dotprod-probe',workflow)
        self.assertIn('WR_BRIDGE_PROBE_PROTOCOL_0496_DOTPROD_V1',workflow)
        self.assertNotIn('gradle',workflow.lower())
        self.assertNotIn('assembledebug',workflow.lower())
        self.assertNotIn('push:',workflow)
        self.assertIn('add_compile_options(-march=armv8.2-a+dotprod)',workflow)

    def test_local_cpp_runtime_guard_and_ui_default(self):
        cpp=(ROOT/'native/qwen_bridge/qwen_npu_llama_probe.cpp').read_text()
        self.assertLess(cpp.index('getauxval(AT_HWCAP)'),cpp.index('llama_backend_init()'))
        self.assertIn('__ARM_FEATURE_DOTPROD',cpp)
        self.assertIn('INCONCLUSIVE: ARM_DOTPROD_CPU_NOT_SUPPORTED',cpp)
        ui=(ROOT/'Start-NpuBuilder.ps1').read_text(encoding='utf-8-sig')
        self.assertIn("$chkBridgeFastQ8.Checked = $true",ui)
        self.assertIn("'--q8-fast-baseline'",ui)
        self.assertIn("'--full-trials',$(if ($useFastQ8) {'1'} else {'3'})",ui)
        self.assertIn('EXPLICIT', str(runmod.run.__doc__)) if runmod.run.__doc__ else None
        self.assertEqual((ROOT/'VERSION.txt').read_text().strip(),'0.4.96')

if __name__=='__main__':
    unittest.main()
