
pushd query-engine\src\test\resources\
docker compose -f query-engine-compose.yml -p query-engine up -d
popd

