# Instructions

```
virtualenv venv3 -p python3
source venv3/bin/activate
pip install tensorflow-gpu=2.1.0 cffi gin-config
cd maml_models
sh create_links_to_latest_models.sh
cd ..
python adhoc_play.py --teammate naive_mlp --games_per_test 5 --num_adhoc_tests 2 --naive_retests 10
python adhoc_play.py --teammate maml --games_per_test 5 --num_adhoc_tests 10
```

It is okay if not all the adhoc maml agents have been trained. The `adhoc_play.py` script will still run with just the agents that have been trained.
