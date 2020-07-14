
for i in {0..19}
do
    saveloc=../replay_data/rainbow/$i
    mkdir $saveloc
    echo "Creating ${i}th batch of data."
    python create_rainbow_data.py --n 25000 --s $saveloc &>/dev/null
done

