#!/bin/zsh

aarSrcPath="./edu/build/outputs/aar/"
aarSrcName="edu-release.aar"
aarDesPath="./app/libs/"
aarDesName="aPaaS-Acadsoc-release.aar"
echo "Start to Clean Project..."
./gradlew clean
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
  echo "Clean Project Success!"
else
  echo "!!!Clean Project Failed!!!"
  exit 1
fi
echo "Start to setup project depends on the AAR file..."
./gradlew -b ./dependAAR.gradle
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
  echo "Setup Success!"
else
  echo "!!!Setup Failed!!!"
  exit 2
fi
echo "Start to build AAR file"
./gradlew edu:assemble
# shellcheck disable=SC2181
if [ $? -eq 0 ]; then
  if [ ! -d "$aarSrcPath" ]; then
    echo "!!!aarSrcPath not exists---AAR build failed!!!"
    exit 4
  fi
  aarSrcFilePath=$aarSrcPath$aarSrcName
  if [ ! -f "$aarSrcFilePath" ]; then
    echo "!!!AAR file not exists---AAR build failed!!!"
    exit 5
  fi
  if [ ! -d "$aarDesPath" ]; then
    mkdir $aarDesPath
  fi
  aarDesFilePath=$aarDesPath$aarDesName
  mv $aarSrcFilePath $aarDesFilePath
  # shellcheck disable=SC2181
  if [ $? -eq 0 ]; then
    if [ ! -f "$aarSrcFilePath" ]; then
      echo "Copy aar File to libs/ success!"
      echo "Start to build apk..."
      ./gradlew app:assembleNormalDebug
      # shellcheck disable=SC2181
      if [ $? -eq 0 ]; then
        apkFilePath="./app/build/outputs/apk/normal/debug/app-normal-debug.apk"
        if [ ! -f "apkFilePath" ]; then
          echo "Apk build success!"
        else
          echo "!!!Apk build failed!!!"
          echo 9
        fi
      else
        echo "!!!Apk build command execute failed!!!"
        exit 8
      fi
    else
      echo "!!!Des aar file not exists!!!"
      exit 7
    fi
  else
    echo "!!!Copy command execute failed!!!"
    exit 6
  fi
else
  echo "!!!Build AAR Failed!!!"
  exit 3
fi
