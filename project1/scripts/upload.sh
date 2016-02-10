#!/usr/bin/env bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "scping jar..."
scp ${DIR}/../out/artifacts/project1_jar/project1.jar lab:/aut/proj/cs572/ygao56/project1
