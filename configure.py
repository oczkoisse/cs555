import sys

collator = None
nodes = []
bin_path = "~/Documents/Assignments/cs555/out/production/cs555"

# Read config file
with open(sys.argv[1]) as config:
    for line in config:
        if not collator:
            collator = line.strip()
        else:
            nodes += [line.strip()]

invocations = []
collator_host, collator_port = collator.split(":")
collator_cmd =  "cd " + bin_path + "; " + "java cs555.a1.nodes.Collator " + collator_port + " ./config0.txt"
if collator_host != "127.0.0.1" and collator_host != "localhost":
    collator_cmd = "ssh " + collator_host + " -t \"" + collator_cmd + "; bash -l\""
invocations += [collator_cmd]
    
# Write separate config files
# First for Collator  as config0.txt
with open('config0.txt', 'w') as cf:
    for n in nodes:
        cf.write(n + '\n')
# Next for all other nodes as config1.txt, config2.txt, and so on.
for i in range(len(nodes)):
    cur_node = nodes[i]
    host, port = cur_node.split(':')
    other_nodes = nodes[:i] + nodes[i+1:]
    with open('config' + str(i+1) + '.txt', 'w') as f:
        for n in other_nodes:
            f.write(n + '\n')
    # Prepare commands while at it
    invocation = ' '.join(["java cs555.a1.nodes.Process", port, collator, "./config" + str(i+1) + ".txt"])
    cmd = "cd " + bin_path + "; " + invocation
    if host != '127.0.0.1' and host != 'localhost':
         cmd = "ssh " + host + " -t \"" +  cmd + "; bash -l\""
    invocations += [ cmd ]

for i in invocations:
    print i


