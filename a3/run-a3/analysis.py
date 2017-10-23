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
    genres = ['rock', 'blues', 'jazz', 'hip hop', 'electronic']
    for genre in genres:
        ys += [[counts[_x][genre] if genre in counts[_x] else 0.0 for _x in x]]

    axes = plt.gca()
    axes.set_ylim([0.0, 1.0])
    plt.xticks(range(1, len(x)+1), x, rotation=45)
    plt.gca().get_xaxis().tick_bottom()

    for i in range(len(ys)):
        plt.plot(range(1, len(x) + 1), ys[i], label=genres[i])
    plt.legend()
    plt.show()


