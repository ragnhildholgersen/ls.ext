#!/usr/bin/env bash
if [ "$#" -ne 3 ]; then
  echo "provision.sh takes exactly three parameters:"
  echo "  provision.sh [lsenv] [lsextpath] [host]"
fi
export LSENV=$1
export LSEXTPATH=$2
export HOST=$3
echo -e "\n Provisioning for $LSENV env, LSENV=$LSENV, LSEXTPATH=$LSEXTPATH, HOST=$HOST\n"
if [[ `uname -s` == 'Linux' ]]; then
  echo -e "\n1) Installing Docker\n"
  VERSION="1.12.1-0~$(lsb_release -c -s)"
  INSTALLED=`dpkg -l | grep docker-engine | awk '{print $3}'`
  if [ $VERSION = "$INSTALLED" ] ; then
    echo "docker version $VERSION already installed";
  else
    echo "Installing docker version $VERSION ...";
    sudo apt-get purge --assume-yes --quiet docker-engine >/dev/null 2>&1 || true
    sudo apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
    echo "deb https://apt.dockerproject.org/repo ubuntu-$(lsb_release -c -s) main" | sudo tee /etc/apt/sources.list.d/docker.list
    sudo apt-get update
    sudo apt-get -y -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold \
      install linux-image-extra-$(uname -r) make git docker-engine=$VERSION
    sudo echo 'DOCKER_OPTS="--storage-driver=aufs"' > /etc/default/docker
    sudo service docker restart
    echo "docker installed."
  fi

  echo -e "\n2) Installing Docker-compose\n"
  COMPOSEVERSION=1.8.0
  INSTALLED=`docker-compose -v | cut -d',' -f1 | cut -d' ' -f3`
  if [ $COMPOSEVERSION = "$INSTALLED" ] ; then
    echo "docker-compose version $COMPOSEVERSION already installed"
  else
    sudo bash -c "curl -s -L https://github.com/docker/compose/releases/download/$COMPOSEVERSION/docker-compose-`uname -s`-`uname -m` > /usr/local/bin/docker-compose"
    sudo chmod +x /usr/local/bin/docker-compose
  fi

  echo -e "\n3) Installing Graphviz\n"
  which dot > /dev/null || sudo apt-get install -y graphviz
else
  echo "Cannot provision for OSX; please install docker & docker-compose yourself"
  echo "You also need envsubst (TODO find a cross-platform solution)"
fi

echo -e "\n4) Making sure secrets.env is present\n"
if [ ! -f "$LSEXTPATH/docker-compose/secrets.env" ]; then
  touch "$LSEXTPATH/docker-compose/secrets.env"
fi

echo -e "\n5) Provisioning system with docker-compose\n"
cd "$LSEXTPATH/docker-compose"
source docker-compose.env
source secrets.env
export OVERVIEW_BUILD_DIR="$LSEXTPATH/redef/overview"

if [ "$LSEXTPATH" = "/vagrant" ]; then
  export MOUNTPATH="/mnt"
else
  export MOUNTPATH=$LSEXTPATH
fi

case "$LSENV" in
  'build')
  envsubst < "docker-compose-template-dev-CI.yml" > "docker-compose.yml"
  ;;
  'prod')
  envsubst < "docker-compose-template-prod.yml" > "docker-compose.yml"
  ;;
  *)
  envsubst < "docker-compose-template-dev.yml" > "docker-compose.yml"
  ;;
esac
sudo docker-compose stop overview && sudo docker-compose rm -f overview
sudo docker-compose up -d

if [ "$LSENV" == "prod" ]; then
  exit 0
fi

echo -e "\n6) Attempting to set up Elasticsearch indices and mappings"
for i in {1..10}; do
  wget --method=POST --retry-connrefused --waitretry=1 --read-timeout=20 --timeout=15 -t 0 -qO- "$HOST:8005/search/clear_index" &> /dev/null
  if [ $? = 0 ]; then break; fi;
  sleep 3s;
done;
