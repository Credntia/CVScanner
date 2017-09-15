# CVScanner
An OpenCV based library for Android to scan/crop ID documents or Passports. 

## Usage
### Automatic Crop
The easiest way is to launch the `DocumentScannerActivity`

```java
CVScanner.startScanner(this, isPassport, REQ_SCAN);
```
You'll get the path of the scanned image in `onActivityResult(int requestCode, int resultCode, Intent data)`

```java
if(requestCode == REQ_SCAN && resultCode == RESULT_OK){
  String path = data.getStringExtra(CVScanner.RESULT_IMAGE_PATH);
}
```

You can use the `DocumentScannerFragment` too

```java
Fragment fragment = DocumentScannerFragment.instantiate(isScanningPassport);
getSupportFragmentManager().beginTransaction()
        .add(R.id.container, fragment)
        .commit();
```
The host Activity should implement `ImageProcessorCallback` to get scanning results.

### Manual Crop
The easiest way is to launch the `CropImageActivity`

```java
CVScanner.startManualCropper(this, currentPhotoUri, REQ_CROP_IMAGE);
```
You'll get the path to the scanned image in `onActivityResult`

```java
if(requestCode == REQ_CROP_IMAGE && resultCode == RESULT_OK){
  String path = data.getStringExtra(CVScanner.RESULT_IMAGE_PATH);
}
```
You can use the `ImageCropperFragment` too

```java
Fragment fragment = ImageCropperFragment.instantiate(imageUri);
getSupportFragmentManager().beginTransaction()
    .add(R.id.container, fragment)
    .commit();
```
The host Activity should implement `ImageProcessorCallback` to get cropping results.
