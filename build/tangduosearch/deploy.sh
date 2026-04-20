#!/bin/bash

#
# Copyright (c) 2026. Aberic - All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

echo deploy start ===========================================================================================================

docker rm -f tangduosearch
docker rmi -f registry.cn-hangzhou.aliyuncs.com/aberic/tangduosearch:latest
cp ../search.jar .
docker build -t registry.cn-hangzhou.aliyuncs.com/aberic/tangduosearch:latest .

docker run --name tangduosearch --restart=always \
-p 19219:19219 \
-p 19220:19220 \
-v /etc/localtime:/etc/localtime \
-v /etc/timezone:/etc/timezone \
-v /data/vol/tangduosearch:/data \
-itd registry.cn-hangzhou.aliyuncs.com/aberic/tangduosearch:latest
echo deploy end ==============================================================================================================
