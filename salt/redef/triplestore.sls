{% set repo = 'fisch42' %}
{% set image = 'fuseki' %}
{% set tag = 'latest' %}
{% set force_pull = false %}

{% include 'docker-pull.sls-fragment' %}

{% set container = 'fuseki_container' %}
{% set ports = ["3030/tcp"] %}
{% set environment = {} %}
{% set port_bindings = {'3030/tcp': { 'HostIp': pillar['redef']['triplestore']['binding'], 'HostPort': pillar['redef']['triplestore']['port'] } } %}
{% set data_volume = { 'container': 'fuseki_data', 'image': 'busybox', 'tag': 'latest', 'volumes': ['/data'] } %}

{% include 'docker-run.sls-fragment' %}