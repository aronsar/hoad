def convert(bin_list):
    """ Convert a binary list into an integer.
    Arguments:
        - vec: list
            List containing 0s and 1s.
    Returns:
        - Converted integer.
    """
    return sum(x << i for i, x in enumerate(reversed(bin_list)))