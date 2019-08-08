def convert(bin_list):
    """ Convert a binary list into an integer.
    Arguments:
        - vec: list
            List containing 0s and 1s.
    Returns:
        - Converted integer.
    """
    return sum(x << i for i, x in enumerate(reversed(bin_list)))

def revert(x, length):
    """ Revert an integer back to a binary list.
    Arguments:
        - x: int
            An integer that is to be reverted back to a binary list.
        - length: int
            Total length of the binary list that will be returned.
            If x >= (2^length), there will be no padding.
    """
    return [int(i) for i in list(format(x, '0%db' % length))]