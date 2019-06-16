#!/bin/bash

deactivate
source venv2/bin/activate
python create_data.py
deactivate
source venv3/bin/activate
