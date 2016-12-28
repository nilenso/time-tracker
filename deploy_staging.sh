#!/usr/bin/env bash

USER=timetracker;
HOST=time.nilenso.com;
ssh $USER@$HOST \
    'source $HOME/env.sh; \
     cd time-tracker; \
     git checkout ci-setup && git pull; \
     killall -9 -v java; \
     ($HOME/lein trampoline run &);
     exit'
