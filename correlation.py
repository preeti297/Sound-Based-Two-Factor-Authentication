# -*- coding: utf-8 -*-
"""
Created on Sun Nov 17 22:20:57 2019

@author: bhanu
"""

# correlation.py

import subprocess
import numpy 
import os

# seconds to sample audio file for
sample_time = 500
#sample_time = 3
# number of points to scan cross correlation over
#Intial Span time
#span = 150
span = 7
# step size (in points) of cross correlation
step = 1
# minimum number of points that must overlap in cross correlation
# exception is raised if this cannot be met
#min_overlap = 20
min_overlap = 2
# report match when cross correlation has a peak exceeding threshold
threshold = 0.5

# calculate fingerprint
def calculate_fingerprints(filename):
    fingerprints = None
    try:
        print("calculate_fingerprints ", filename)
        fpcalc_out = subprocess.check_output("./fpcalc -raw -length %i %s"
                                    % (sample_time, filename),stderr=subprocess.STDOUT,shell = True)

        # print(fpcalc_out)
        # To convert bytes to string
        fpcalc_out = fpcalc_out.decode("utf-8")
        # print(fpcalc_out)
        # print(fpcalc_out.find("FINGERPRINT="))

        fingerprint_index = fpcalc_out.find("FINGERPRINT=") + 12

        # convert fingerprint to list of integers
        fingerprints = map(int, fpcalc_out[fingerprint_index:].split(','))
    except subprocess.CalledProcessError as exc:
        print("Status : FAIL", exc.returncode, exc.output)

    return fingerprints
    #return []

  
# returns correlation between lists
def correlation(listx, listy):
    if len(listx) == 0 or len(listy) == 0:
        # Error checking in main program should prevent us from ever being
        # able to get here.
        raise Exception('Empty lists cannot be correlated.')
    if len(listx) > len(listy):
        listx = listx[:len(listy)]
    elif len(listx) < len(listy):
        listy = listy[:len(listx)]
    
    covariance = 0
    for i in range(len(listx)):
        covariance += 32 - bin(listx[i] ^ listy[i]).count("1") #purpose of 32?
    covariance = covariance / float(len(listx))
    
    return covariance/32
  
# return cross correlation, with listy offset from listx
def cross_correlation(listx, listy, offset):
    if offset > 0:
        listx = listx[offset:]
        listy = listy[:len(listx)]
#        print("If");
#        print("X",listx)
#        print("Y",listy)
    elif offset < 0:
        offset = -offset
        listy = listy[offset:]
        listx = listx[:len(listy)]
#        print("elif");
#        print("X",listx)
#        print("Y",listy)
    if min(len(listx), len(listy)) < min_overlap:
        print(len(listx),len(listy))
        print("min_overlap not satisfied")
        # Error checking in main program should prevent us from ever being
        # able to get here.
        return 
    #raise Exception('Overlap too small: %i' % min(len(listx), len(listy)))
    return correlation(listx, listy)
  
# cross correlate listx and listy with offsets from -span to span
def compare(listx, listy, span, step):
    if span > min(len(listx), len(listy)):
        # Error checking in main program should prevent us from ever being
        # able to get here.
        raise Exception('span >= sample size: %i >= %i\n'
                        % (span, min(len(listx), len(listy)))
                        + 'Reduce span, reduce crop or increase sample_time.')
    corr_xy = []
    for offset in numpy.arange(-span, span + 1, step): 
        corr_xy.append(cross_correlation(listx, listy, offset))
    return corr_xy
  
# return index of maximum value in list
def max_index(listx):
    max_index = 0
    max_value = listx[0]
    for i, value in enumerate(listx):
        if value > max_value:
            max_value = value
            max_index = i
    return max_index
  
def get_max_corr(corr, source, target):
    max_corr_index = max_index(corr)
    max_corr_offset = -span + max_corr_index * step
    print("max_corr_index = ", max_corr_index, "max_corr_offset = ", max_corr_offset)
# report matches
    if corr[max_corr_index] > threshold:
        print('%s and %s match with correlation of %.4f at offset %i'
             % (source, target, corr[max_corr_index], max_corr_offset))
        print("Correlation Score: ", corr[max_corr_index])
        return corr[max_corr_index]
    print("Correlation Score: ", corr[max_corr_index])
    return -1

def correlate(source, target):
    
    fingerprint_source = calculate_fingerprints(source)
    fingerprint_target = calculate_fingerprints(target)

    fingerprint_source = list(fingerprint_source)
    fingerprint_target = list(fingerprint_target)
    
    print(fingerprint_source)
    print(fingerprint_target)

    corr = compare(fingerprint_source, fingerprint_target, span, step)
    correlation_score = get_max_corr(corr, source, target)
    return correlation_score