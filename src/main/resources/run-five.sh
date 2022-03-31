#!/bin/bash

for i in 1 2 3 4 5
do
  ./$1
  mkdir run\_$i
  cp -r exp\_* run\_$i
  rm -rf exp\_*
done