# CVScanner
An OpenCV based library for Android to scan/crop ID documents or Passports. 

## Usage
### Auto Crop
The easiest way is to launch the `DocumentScannerActivity`

```java
Intent i = new Intent(context, DocumentScannerActivity.class);
i.putExtra(DocumentScannerActivity.IsScanningPassport, true);
startActivityForResult(i, REQ_SCAN);
```
You'll get the uri of the scanned image in `onActivityResult(int requestCode, int resultCode, Intent data)`

```java
if(data != null && data.getData() != null){
  Uri scannedImageUri = data.getData();                      
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
Start the `CropImageActivity` with an image uri to crop that manually with the help of an adjustable trapezoid.

```java
Intent intent = new Intent(this, CropImageActivity.class);

intent.putExtra(CropImageActivity.EXTRA_IMAGE_URI, currentPhotoUri.toString());
startActivityForResult(intent, REQ_CROP_IMAGE);
```

You'll get back the cropped image's uri in `onActivityResult(int requestCode, int resultCode, Intent data)`

```java
if(data != null && data.getData() != null){
  Uri croppedImageUri = data.getData();                      
}
```

You can also use the `ImageCropperFragment`.
