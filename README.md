# Asleep SDK Android Sample — `sample/setup-product-beginend-polling`

> This branch is a variant of the [default branch](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/main).
> See the default branch README for the full list of variants.

> **Requires a server-side plan.** Product registration only works when your contract includes
> product-based billing. Contact **platform-cs@asleep.ai** to enable it on your plan.

## What this branch demonstrates

Registering the device as a **product with `Asleep.setup()` + `ProductInfo`** before the app initializes.

When `productInfo` is passed to `Asleep.setup()`, setup acts as a **gate**: the device is registered as a billable product first, and SDK configuration continues only after registration succeeds. The issued credential is cached by the SDK, so **the registration request goes to the server only once** — later launches call `onComplete` without any network call.

| Topic | default | This branch |
|---|---|---|
| Initialization | `initAsleepConfig(apiKey = ...)` directly | `Asleep.setup(..., productInfo = ...)` first, then `initAsleepConfig()` |
| Authentication | API Key | same (API Key) |
| Tracking | `Asleep.beginSleepTracking()` / `endSleepTracking()` | same |
| Callbacks | `Asleep.AsleepTrackingListener` | same |
| Interim results | Polling with `Asleep.getCurrentSleepData()` | same |

## Changes from default

### `ui/main/AsleepViewModel.kt`

Adds `setupAsleep()` and routes both entry points (`initAsleepConfig()` / `beginAutoSleepTracking()`) through the gate.

```kotlin
Asleep.setup(
    context = applicationContext,
    apiKey = Constants.ASLEEP_API_KEY,
    baseUrl = Constants.BASE_URL,
    callbackUrl = Constants.CALLBACK_URL,
    service = Constants.SERVICE_NAME,
    asleepSetupListener = object : Asleep.AsleepSetupListener {
        override fun onComplete() { /* registered -> proceed to initAsleepConfig() */ }
        override fun onProgress(progress: Int) { /* progress log */ }
        override fun onFail(errorCode: Int, detail: String) { /* handle 13000 / 13400 */ }
    },
    productInfo = ProductInfo(
        model = Constants.PRODUCT_MODEL,
        identifierType = Asleep.ProductIdentifierType.SERIAL,
        identifierValue = PreferenceHelper.getOrCreateProductSerial(applicationContext)
    )
)
```

- `onComplete()` continues into the existing `initAsleepConfig()` flow.
- The former `initAsleepConfig()` body is extracted into `requestAsleepConfig()` so both paths share it.

### `utils/PreferenceHelper.kt`

Adds `getOrCreateProductSerial()` — generates a UUID once, stores it, and reuses it afterwards. The identifier **must be a stable value**: if it changes on every launch, a new product is registered each time. For a real product, use the device's engraved serial or MAC address.

### `utils/AsleepErrorUtils.kt`

Adds guidance messages for the product registration failure codes.

| Code | Constant | Meaning |
|---|---|---|
| `13000` | `ERR_PRODUCT_REGISTER_FAILED` | Transient failure (network / 5xx). Calling setup again may succeed |
| `13400` | `ERR_PRODUCT_REGISTER_REJECTED` | Permanent rejection (4xx: bad input, credentials, or no permission). Retrying will not help |

Both codes transition to `STATE_ERROR` and show the guidance in the existing error dialog.

### `ui/Constants.kt`

Adds `PRODUCT_MODEL = "model-123"` — **a placeholder.** Replace it with your issued model name for a real integration.

## Setup

1. Add your issued API Key to `local.properties` in the project root.

   ```properties
   asleep_api_key="YOUR_API_KEY"
   ```

2. Replace `PRODUCT_MODEL` in `ui/Constants.kt` with your issued model name.

   ```kotlin
   const val PRODUCT_MODEL = "model-123"  // replace with your model name
   ```

3. The identifier (`identifierValue`) is a UUID the app generates and stores in `SharedPreferences`. For a real product, replace it with the device's unique serial or MAC address, and set `identifierType` to `SERIAL` or `MAC_ADDRESS` accordingly.

4. Run in Android Studio.

> Registration success shows up in the log as `setup onComplete`. If relaunching the app fires `onComplete` immediately without a server call, the credential cache is working.

## See also

- [Asleep SDK Android Docs](https://docs.asleep.ai/docs/android)
- [Back to the default branch](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/main)
