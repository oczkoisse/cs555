import matplotlib.pyplot as plt
import sys

def get_results_reader(file_path):
    with open(file_path) as f:
        for l in f:
            decade_genre, count = l.split('\t')
            decade, genre = decade_genre.split(';')
            yield decade, genre, int(count)

def get_counts():
    counts = {}
    reader = get_results_reader('analysis_results.tsv')
    total_g_counts = 0
    for d, g, c in reader:
        if d not in counts:
            counts[d] = {}
        counts[d][g] = c
    return counts

if __name__ == '__main__':
    counts = get_counts()
    for d in counts.keys():
        total_g_in_d = sum(counts[d].values())
        for g in counts[d].keys():
            counts[d][g] /= float(total_g_in_d)

    x = sorted(counts.keys())
    ys = []
    genres = sys.argv[1].split(';')
    genres = [ g.strip() for g in genres ]
    for genre in genres:
        ys += [[counts[_x][genre] if genre in counts[_x] else 0.0 for _x in x]]

    print ys
    axes = plt.gca()
    axes.set_ylim([0.0, 1.0])
    plt.xticks(range(len(x)), x, rotation=45)
    plt.gca().get_xaxis().tick_bottom()
    plt.show()


