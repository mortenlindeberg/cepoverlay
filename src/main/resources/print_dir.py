#!/usr/bin/python

def to_wstrat_name(wstrat, wait):
    if wstrat == 'false' and not wait:
        return '0 - None'

    if wstrat == 'true':
        return '1 - Aware'

    if wstrat == 'false' and wait:
        return '2 - Wait'

def print_dir(exp_dir):
    run = exp_dir.split('run_')[1].split('/')[0]
    strat = exp_dir.split('_')[4].split('_')[0]
    if (strat == '27'):
        win = 0
    else:
        win = exp_dir.split('_')[5].split('_')[0]
    wstrat = to_wstrat_name(exp_dir.split('_')[6].split('_')[0], len(exp_dir.split('_')) > 7)

    return '%s & %s & %s & %s' % (run, strat, win, wstrat)

