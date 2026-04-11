#!/bin/zsh

set -e

PATH_TO_FX=/Users/ktr/javafx/javafx-sdk-21.0.10/lib

mkdir -p out
javac --module-path "$PATH_TO_FX" --add-modules javafx.controls,javafx.fxml -d out $(find src -name "*.java")
java --module-path "$PATH_TO_FX" --add-modules javafx.controls,javafx.fxml -cp out game.Main
