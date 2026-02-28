# cursorGrid
- カーソルテーマをグリッド画像に変換
- カーソル画像ひとつをPNGにデコード
- 将来はPNGデータをxcursor形式にエンコードする機能を追加するかもしれない

# 入力データの例
Linux なら以下のフォルダにカーソルテーマがないか探してください
- ls /usr/share/icons/
- ls ~/.icons/

# Usage
```
java -jar cursorGrid.jar (options) <action> {action-args...}
```
## Global Options:
* -v 冗長出力
* -h help

## grid :アイコンテーマをグリッド画像に出力
```
java -jar cursorGrid.jar grid inFolder outFile
```
- inFolderはXウィンドウのカーソルテーマの1つを指定する。
  - カーソルテーマのフォルダ、または圧縮されたzipファイル
- カーソル画像を名前順ソート、デコード、グリッド状に並べたPNG画像を出力する
- テーマのインデクスの対応サイズなどを標準出力に列挙

## xcur2png :xcursorファイルのデコード
```
java -jar cursorGrid.jar xcur2png inFile outFile  
```
- inFile :XCursor形式のアイコン画像データ
- outFile :出力ファイル(複数)のprefix
  - 実際には以下のファイルが生成される。
    - 解像度別のPNG画像（ARGB）
    - png2xcurに必要なJSONデータ

## png2xcur :xcursorファイルのエンコード
```
java -jar cursorGrid.jar png2xcur [options] inJson outFile  
```
- inJson :png2xcur で出力したJSONデータ
- outFile :XCursor形式のアイコン画像データ
