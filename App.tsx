import React, { useEffect, useState } from 'react';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import {
  Linking,
  Alert,
  View,
  Text,
  Pressable,
  StyleSheet,
  Image,
  ScrollView,
} from 'react-native';
import storage from '@react-native-firebase/storage';

// 🔗 AR açıcı
async function openAR(model?: string) {
  const url = model ? `veonoes://ar?model=${model}` : 'veonoes://ar';
  const ok = await Linking.canOpenURL(url);
  if (!ok) {
    Alert.alert(
      'Açılamadı',
      'AR aktivitesi bulunamadı. Manifest’teki deeplink’i kontrol et.',
    );
    return;
  }
  await Linking.openURL(url);
}

console.log('Storage bucket:', storage().app.options.storageBucket);

// 🔲 Kart bileşeni
function Card({
  title,
  image,
  onPress,
}: {
  title: string;
  image: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [styles.card, pressed && { opacity: 0.7 }]}
    >
      <Image
        source={{ uri: image }}
        style={styles.image}
        resizeMode="contain"
      />
      <Text style={styles.cardTitle}>{title}</Text>
    </Pressable>
  );
}

// 🧭 Ana Uygulama
export default function App() {
  const [glasses, setGlasses] = useState<{ name: string; url: string }[]>([]);

  useEffect(() => {
    const loadGlasses = async () => {
      try {
        console.log('Default bucket:', storage().app.options.storageBucket);

        // 🔹 Varsayılan Storage referansı
        const rootRef = storage().ref('/');
        const res = await rootRef.listAll();

        console.log('Toplam dosya:', res.items.length);

        if (res.items.length === 0) {
          Alert.alert('Uyarı', 'Storage içinde gözlük dosyası bulunamadı.');
          return;
        }

        // 🔹 Dosyaları al
        const items = await Promise.all(
          res.items.map(async item => {
            const url = await item.getDownloadURL();
            console.log('Dosya indirildi:', item.name);
            return { name: item.name.replace('.png', ''), url };
          }),
        );

        setGlasses(items);
      } catch (err: any) {
        console.error('Storage Hatası:', err);
        Alert.alert(
          'Bağlantı hatası',
          `Firebase Storage erişiminde hata oluştu:\n${err.message}`,
        );
      }
    };

    loadGlasses();
  }, []);

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.container}>
        <Text style={styles.h1}>AR Gözlük</Text>
        <Text style={styles.p}>
          Firebase Storage’dan dinamik olarak yükleniyor.
        </Text>

        <ScrollView contentContainerStyle={styles.scroll}>
          <View style={styles.row}>
            {glasses.map(g => (
              <Card
                key={g.name}
                title={g.name}
                image={g.url}
                onPress={() => openAR(g.url)}
              />
            ))}
          </View>
        </ScrollView>

        <Pressable
          onPress={() => openAR()}
          style={({ pressed }) => [styles.cta, pressed && { opacity: 0.8 }]}
        >
          <Text style={styles.ctaText}>AR Modunu Aç</Text>
        </Pressable>
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

// 💅 Stiller
const styles = StyleSheet.create({
  container: { flex: 1, padding: 16 },
  h1: { fontSize: 24, fontWeight: '700', marginBottom: 4 },
  p: { fontSize: 14, opacity: 0.7, marginBottom: 12 },
  scroll: { flexGrow: 1 },
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
    justifyContent: 'center',
  },
  card: {
    width: 150,
    alignItems: 'center',
    padding: 10,
    borderRadius: 10,
    backgroundColor: '#f4f6f9',
    marginBottom: 12,
  },
  image: { width: 100, height: 50, marginBottom: 8 },
  cardTitle: { fontWeight: '600', fontSize: 14, textAlign: 'center' },
  cta: {
    marginTop: 'auto',
    backgroundColor: '#2563eb',
    padding: 16,
    borderRadius: 14,
    alignItems: 'center',
  },
  ctaText: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
