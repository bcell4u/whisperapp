#!/usr/bin/env python3
"""Fail-closed multi-phase report consolidator; missing phases remain inconclusive."""
import argparse,json
from pathlib import Path

NAMES={'storage':'WEIGHT_IDENTITY_REPORT.json','qk':'QK_LAYOUT_REPORT.json','positions':'HF_POSITION_AXES_REPORT.json'}
def evaluate(folder):
    folder=Path(folder);phases={}
    for phase,file in NAMES.items():
        path=folder/file
        try:
            data=json.loads(path.read_text());
            if data.get('release')!='6.6.24' or not isinstance(data.get('status'),str):
                raise ValueError('REPORT_SCHEMA_OR_VERSION_MISMATCH')
            phases[phase]={'status':data['status'],'available':True}
            if phase=='storage':
                phases[phase]['audited_tensors']=data.get('equivalent_audited_tensors')
                phases[phase]['unmapped_tensors']=data.get('unmapped_gguf_count')
            elif phase=='qk':
                phases[phase]['qk_tensors']=data.get('qk_tensor_count')
                phases[phase]['match_counts']=data.get('match_counts')
            else:
                phases[phase]['baseline_top1']=data.get('baseline_2d_top1')
                phases[phase]['position_input_shapes']={k: v.get('status') for k,v in data.get('variants',{}).items()}
                phases[phase]['direct_rotary_status']=data.get('direct_rotary',{}).get('status')
        except (FileNotFoundError,ValueError,TypeError,KeyError,json.JSONDecodeError):
            phases[phase]={'status':'INCONCLUSIVE_MISSING_OR_INVALID_PHASE_REPORT','available':False}
    if not all(p['available'] for p in phases.values()):status='PARTIAL_PHASE_FAILURE'
    elif phases['storage']['status']=='AUDITED_TENSORS_DIFFER':status='CONFIRMED_AUDITED_STORAGE_DIFFERENCE'
    elif phases['storage']['status'].startswith('INCONCLUSIVE') or phases['qk']['status'].startswith('INCONCLUSIVE') or phases['positions']['status'].startswith('INCONCLUSIVE'):
        status='COMPLETED_WITH_UNRESOLVED_CONTROLS'
    else:status='MULTIPHASE_CONTROLS_COMPLETED'
    return {'schema':1,'release':'6.6.24','status':status,'phases':phases,
            'full_model_identity':'NOT_ESTABLISHED','official_mrope_parity':'UNQUALIFIED',
            'phone_decoder_changed':False,'synthetic_only':True,
            'next_decision':'Inspect unresolved layout/position evidence; do not change Android inference without independent correctness and accuracy tests'}

def main():
    p=argparse.ArgumentParser();p.add_argument('--artifact-dir',required=True);p.add_argument('--output',required=True)
    x=p.parse_args(); report=evaluate(x.artifact_dir)
    Path(x.output).parent.mkdir(parents=True,exist_ok=True)
    Path(x.output).write_text(json.dumps(report,indent=2,sort_keys=True)+'\n');print('MULTIPHASE',report['status'])
if __name__=='__main__':main()
