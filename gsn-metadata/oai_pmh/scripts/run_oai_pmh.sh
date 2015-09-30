DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

cd $DIR

cd ../oai-pmh-interfaces/
mvn clean install
cd ../oai-pmh-swissex/
mvn clean install
cd ../oai-rest/
mvn clean install

nohup java -jar target/oai-pmh-rest-0.1.0.jar > ../logs/oai-pmh.log &