# श्री गणेश संगीत यन्त्र — Android APK (GitHub Actions)

## फोल्डर (५) र फाइल
```
.github/workflows/build-apk.yml   APK बनाउने workflow
native/java/*.java                Android कोड (अलार्म, सेवा, plugin) — ८ फाइल
native/res/mipmap-*/*.png         एपको आइकन
player/index.html                 मुख्य प्लेयर
scripts/prepare-www.js, patch-android.js
signing/debug.keystore            APK सही (sign) गर्ने कुञ्जी
package.json, capacitor.config.json, .gitignore, README.md
```

## APK बनाउने तरिका
1. GitHub मा नयाँ repository बनाउनुहोस्।
2. यो ZIP खोलेर भित्रका सबै फाइल र फोल्डर repository मा तानेर upload गर्नुहोस् (`.github` लुकेको फोल्डर हो — पछि जाँच्नुहोस्)।
3. **Actions → Build APK** चल्छ (नचले *Run workflow*)। ८–१५ मिनेट लाग्छ।
4. सकिएपछि run खोल्नुहोस् → तल **Artifacts → ganesh-sangeet-apk** → zip खोलेर `ganesh-sangeet.apk` install गर्नुहोस्।

## नयाँ के छ
- **अलार्म (रेडियो):** स्क्रिन लक भएको / एप बन्द भएको बेला पनि Android आफैंले समयमा रेडियो बजाउँछ (अवधि सकिएपछि आफैं बन्द)। रेडियो नबजे सामान्य अलार्म टोन बज्छ।
- **लक स्क्रिनमा गीत/रेडियो:** बज्दा एपलाई जीवित राख्ने सेवा चल्छ।
- **रेडियो आफैं जोडिने:** स्ट्रिम टुटे फेरि जोडिन्छ।
- **१० Now Playing थिम** र एनालग घडी।

## पहिलो पटक एप खोल्दा
- Notification को अनुमति दिनुहोस्।
- अलार्म विन्डो भित्रको **🔋 ब्याट्री सेटिङ** थिचेर "अनुमति दिनुहोस्/Unrestricted" गर्नुहोस् (फोनका केही कम्पनीले एप सुताउँछन्)।

## सीमा
- नेटिभ अलार्मले **रेडियो** अलार्म मात्र चलाउँछ। प्लेलिस्ट/फोनको गीतको अलार्म एप खुला हुँदा मात्र बज्छ। अलार्ममा रेकर्ड एप खुला हुँदा मात्र चल्छ।
- पुरानो APK सँग कुञ्जी (keystore) फरक भएकाले पुरानो एप पहिले uninstall गर्नुपर्छ (Favorites/सेटिङ हराउँछ)।
