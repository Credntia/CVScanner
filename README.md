# CVScanner
An OpenCV based library for Android to scan ID documents or Passports. 

## Usage
The easiest way is to launch the `DocumentScannerActivity`

```java
Intent i = new Intent(context, DocumentScannerActivity.class);
i.putExtra(DocumentScannerActivity.IsScanningPassport, true);
startActivityForResult(i, REQ_SCAN);
```
You'll get the path to the scanned image in `onActivityResult`

```java
if(requestCode == REQ_SCAN && resultCode == RESULT_OK){
  String path = (data != null && data.getExtras() != null)? data.getStringExtra(DocumentScannerActivity.ImagePath):null;
}
```
You can use the `DocumentScannerFragment` too

```java
Fragment fragment = DocumentScannerFragment.instantiate(isScanningPassport);
getSupportFragmentManager().beginTransaction()
        .add(R.id.container, fragment)
        .commit();
```
The host Activity should implement `DocumentScannerCallback` to get scanning results.
