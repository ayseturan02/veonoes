import React from 'react';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import {
  Linking,
  Alert,
  View,
  Text,
  Pressable,
  StyleSheet,
} from 'react-native';

// 🔗 AR Deeplink açıcı
async function openAR(model?: 'black' | 'gold' | 'round' | 'blackfull') {
  const url = model ? `veonoes://ar?model=${model}` : 'veonoes://ar';
  const ok = await Linking.canOpenURL(url);
  if (!ok) {
    Alert.alert(
      'Açılamadı',
      'AR aktivitesi bulunamadı. Lütfen Manifest içindeki deeplink’i kontrol et.',
    );
    return;
  }
  await Linking.openURL(url);
}

// 🧱 Kart bileşeni
function Card({
  title,
  subtitle,
  onPress,
}: {
  title: string;
  subtitle?: string;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => [styles.card, pressed && { opacity: 0.7 }]}
    >
      <Text style={styles.cardTitle}>{title}</Text>
      {subtitle ? <Text style={styles.cardSub}>{subtitle}</Text> : null}
    </Pressable>
  );
}

// 🧭 Ana Uygulama
export default function App() {
  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.container}>
        <Text style={styles.h1}>AR Gözlük</Text>
        <Text style={styles.p}>Bir model seç veya doğrudan AR moduna geç.</Text>

        <View style={styles.row}>
          <Card
            title="Klasik Siyah"
            subtitle="Dikdörtgen"
            onPress={() => openAR('black')}
          />
          <Card
            title="Gold Aviator"
            subtitle="Pilot çerçeve"
            onPress={() => openAR('gold')}
          />
        </View>

        <View style={styles.row}>
          <Card
            title="Yuvarlak Retro"
            subtitle="Vintage tarz"
            onPress={() => openAR('round')}
          />
          <Card
            title="Tam Çerçeveli Siyah"
            subtitle="Yeni model"
            onPress={() => openAR('blackfull')}
          />
        </View>

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

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, gap: 16 },
  h1: { fontSize: 24, fontWeight: '700', marginBottom: 4 },
  p: { fontSize: 14, opacity: 0.7, marginBottom: 12 },
  row: {
    flexDirection: 'row',
    gap: 12,
    justifyContent: 'space-between',
  },
  card: {
    flex: 1,
    padding: 12,
    borderRadius: 12,
    backgroundColor: '#f2f4f7',
    borderWidth: 1,
    borderColor: '#e5e7eb',
    minHeight: 90,
    justifyContent: 'center',
  },
  cardTitle: { fontSize: 16, fontWeight: '700' },
  cardSub: { fontSize: 12, opacity: 0.6, marginTop: 4 },
  cta: {
    marginTop: 'auto',
    backgroundColor: '#2563eb',
    padding: 16,
    borderRadius: 14,
    alignItems: 'center',
  },
  ctaText: { color: '#fff', fontWeight: '700', fontSize: 16 },
});
