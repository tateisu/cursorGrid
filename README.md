# cursorGrid

- XWindow の カーソルテーマをグリッド画像に変換
- カーソル画像1つをPNG画像(解像度別,アニメーションフレーム別)にデコード
- PNG 画像の詳細を含むJSONデータからカーソル画像をエンコード

----

# 入力データの例

Linux なら以下のフォルダにカーソルテーマがあるかもしれません

- ls /usr/share/icons/
- ls ~/.icons/

----

# Usage

```
java -jar cursorGrid.jar (options) <action> {action-args...}
```

## Global Options:

* -v 冗長出力
* -h help

## grid :アイコンテーマをグリッド画像に出力

```
java -jar cursorGrid.jar grid [-b color] [-F fontFile] inFolder outFile
```

- inFolderはXウィンドウのカーソルテーマの1つを指定する。
    - カーソルテーマのフォルダ、または圧縮されたzipファイル
- カーソル画像を名前順ソート、デコード、グリッド状に並べたPNG画像を出力する
- テーマのインデクスの対応サイズなどを標準出力に列挙
- -b color :背景色を指定（デフォルト: 757575）
    - `RRGGBB` または `#RRGGBB` :不透明色
    - `AARRGGBB` または `#AARRGGBB` :アルファ値付き
    - `transparent` :透明
- -F fontFile :フォントファイルを指定（TTF/OTF、デフォルト: sans-serif）

## xcur2png :XCursorファイルのデコード

```
java -jar cursorGrid.jar xcur2png [-f] inFile outDir  
```

- inFile :XCursor形式のアイコン画像データ
- outDir :出力ディレクトリ
    - ディレクトリが存在しない場合は作成される
    - ディレクトリ以外のパスが存在する場合はエラー
- -f :既存ファイルを上書きする
    - 指定しない場合、出力ファイルが既に存在するとエラー
- 出力ファイル:
    - 解像度別のPNG画像（ARGB）
    - `png2xcur` に必要な JSONデータ

## png2xcur :XCursorファイルのエンコード

```
java -jar cursorGrid.jar png2xcur [options] inJson outFile  
```

- inJson :png2xcur で出力したJSONデータ
- outFile :XCursor形式のアイコン画像データ

----

# xcur2png, png2xcur で扱うJSONデータの構造

JSONデータは `XCursorImageMeta` の配列です。

```json
[
  {
    "size": 24,
    "width": 24,
    "height": 24,
    "xHot": 4,
    "yHot": 4,
    "delay": 0,
    "pngFile": "left_ptr_24.png"
  },
  {
    "size": 32,
    "width": 32,
    "height": 32,
    "xHot": 6,
    "yHot": 6,
    "delay": 0,
    "pngFile": "left_ptr_32.png"
  }
]
```

## プロパティ

| プロパティ     | 型       | 説明                           |
|-----------|---------|------------------------------|
| `size`    | Int     | カーソルの名目上のサイズ（ピクセル）           |
| `width`   | Int     | 画像の幅（ピクセル）                   |
| `height`  | Int     | 画像の高さ（ピクセル）                  |
| `xHot`    | Int     | ホットスポットのX座標                  |
| `yHot`    | Int     | ホットスポットのY座標                  |
| `delay`   | Int     | アニメーションフレームの表示時間（ミリ秒、デフォルト0） |
| `pngFile` | String? | PNGファイルのパス（JSONファイルからの相対パス）  |

- Note: png2xcur で pngFileを読む際は画像データはJSONに書かれたwidth,heightに自動的にリサイズされる。

## ワークフロー例

1. `xcur2png` でカーソルファイルを PNGとJSONに分解
2. 高解像度の PNG画像を画像編集ソフトで修正
3. JSON内の `pngFile` を修正した画像のパスに変更
4. `png2xcur` で再エンコード
