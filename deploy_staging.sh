#!/usr/bin/env bash

USER=timetracker
HOST=time.nilenso.com
ssh $USER@$HOST 'touch test.foo'
