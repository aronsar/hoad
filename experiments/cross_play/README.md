# Cross-play Scores

The purpose of this experiment is to show that all of the imitator agents have
high self-play scores, but low cross-play scores. This is important in a
meta-learning setting, where it is necessary to have many agents with high
quality but different strategies.

To run this experiment, we'll use the same virtual environment as the imitator 
agents.

```
source ../../imitator_agents/venv3/bin/activate
pip install keras, cffi
python cross_play.py --num_games 100 
        --pattern "../../imitator_agents/saved_models/*.save/best.h5"
``` 

The row/column labels are: holmesbot, iggi, outer, piers, rainbow, simplebot, 
van-den-bergh.

```
     hol    igg    out    pie    rai    sim    vdb
    +------+------+------+------+------+------+-----+
hol | 10.4 | 2.2  | 0    | .8   | 1.4  | 10.8 | 0   |
    +------+------+------+------+------+------+-----+
igg | 1.5  | 14.6 | 3    | 12   | 4.7  | 2.7  | 4.5 |
    +------+------+------+------+------+------+-----+
out | 0    | 1.2  | 13.7 | 4.5  | 9    | 0    | 9.3 |
    +------+------+------+------+------+------+-----+
pie | .92  | 12   | 5.8  | 14.5 | 6.6  | 2.2  | 4.9 |
    +------+------+------+------+------+------+-----+
rai | .2   | 3.3  | 8.4  | 7.5  | 17.6 | .4   | 1.9 |
    +------+------+------+------+------+------+-----+
sim | 12.9 | 3.7  | 0    | 1.2  | 2    | 17   | 0   |
    +------+------+------+------+------+------+-----+
vdb | 0    | 1.8  | 9.2  | 3.8  | 2.16 | 0    | 8.3 |
    +------+------+------+------+------+------+-----+
```
