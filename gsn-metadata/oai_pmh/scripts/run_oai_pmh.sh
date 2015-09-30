DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

cd $DIR

cd ../oai-pmh-interfaces/
mvn -Dmaven.test.skip=true clean install
cd ../oai-pmh-swissex/
mvn -Dmaven.test.skip=true clean install
cd ../oai-rest/
mvn -Dmaven.test.skip=true clean install

nohup java -jar target/oai-pmh-rest-0.1.0.jar > ../logs/oai-pmh.log &