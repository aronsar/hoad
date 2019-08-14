# pseudo-code to implement two stage transfer learning

'''
- algorithm from Stone and Rosenfeld 2013
- T is target data set
- S is set of source data sets S = {S_1, S_2, ..., S_n}
- m is num of boosting iterations
- k is num of folds for cross validation, k should be 10 as we have 10 games
- b is max num of source data sets to include
- S^w means data set S taken with weight w spread over instances
- F is ??? #FIXME

TwoStageTransfer (T, S, m, k, b)
    for all S_i in S: do
        wi ← CalculateOptimalWeight(T, ∅, Si, m, k)
    Sort S in decreasing order of w_i's
    F ← ∅
    for i from 1 to b do
        w ← CalculateOptimalWeight(T, F, S_i, m, k)
        F ← F ∪ S^w_i
    Train classifier c on T ∪ F
    return c

CalculateOptimalWeight(T, F, S, m, k):
    for i from 1 to m do
        w_i = (len(T) / (len(T) + len(S))) *  (1 − i /(m − 1))
    Calculate erri from k-fold cross validation on T using F and S wi as additional training data
    return wj such that j = argmaxi(erri)
'''

def two_stage_transfer(target_data_set, source_data_sets, num_boosting_iter, num_cross_val_folds, max_num_source_data_sets):
    weights = []
    for data_set in source_data_sets:
        #phi is an empty set
        weights.append(calculate_optimal_weight(target_data_set, [], data_set, num_boosting_iter, num_cross_val_folds)
    sort(S in dec order of weights)
    F = []
    
    for set_num in range(1, max_num_source_data_sets):
        weight = calculate_optimal_weight(target_data_set, F, source_data_sets[set_num], num_boosting_iter, num_cross_val_folds)
        F = F union source_data_sets[set_num] ^ weight
    train classifier on target_data_set  union F
    return classifier


# FIXME: rename F accordingly and update above
def calculate_optimal_weight(target_data_set, F, source_data_sets, num_boosting_iter, num_cross_val_folds):
    weights = []
    for boosting_iter in range(1, num_boosting_iter):
        weights[boosting_iter] = (len(target_data_set) / (len(target_data_set) + len(source_data_sets))) * (1 - (boosting_iter / (num_boosting_iter - 1)))
    #find the index of the maximum error and return the weight at that index
    error = []
    for boosting_iter in range(1, num_boosting_iter):
        error[boosting_iter] = error(k-fold cross validtion on T using F and S^{w_i} as additional training data)

    return weights[j | j == argmax(error[i])]


def parse_args(args):
    x = 5 #filler code

if __name__ = '__main__':
    T, S, m, k, b = parse_args(args)
    classifier = two_stage_transfer(T, S, m, 10, b)
    return classifier

