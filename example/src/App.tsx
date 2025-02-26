/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */
import React from 'react';
import { useCallback, useState } from 'react';
import {
  Image,
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableHighlight,
  View,
} from 'react-native';
import Voice, {
  type SpeechErrorEvent,
  type SpeechRecognizedEvent,
  type SpeechResultsEvent,
} from 'react-native-voice-turbo';

const locale = 'ja-JP';
function App(): React.JSX.Element {
  const [recognized, setRecognized] = useState<string>();
  const [pitch, setPitch] = useState<string>();
  const [error, setError] = useState<string>();
  const [end, setEnd] = useState<string>();
  const [started, setStarted] = useState<string>();
  const [results, seResults] = useState<string[]>([]);
  const [partialResults, setPartialResults] = useState<string[]>([]);

  const clearState = useCallback(() => {
    setRecognized('');
    setPitch('');
    setError('');
    setStarted('');
    setEnd('');
    setPartialResults([]);
    seResults([]);
  }, []);

  const onSpeechStart = useCallback((e: any) => {
    console.log('onSpeechStart: ', e);
    setStarted('√');
  }, []);

  const onSpeechRecognized = useCallback((e: SpeechRecognizedEvent) => {
    console.log('onSpeechRecognized: ', e);
    setRecognized('√');
  }, []);

  const onSpeechEnd = useCallback((e: any) => {
    console.log('onSpeechEnd: ', e);
    setEnd('√');
  }, []);

  const onSpeechError = useCallback((e: SpeechErrorEvent) => {
    console.log('onSpeechError: ', e);
    setError(JSON.stringify(e.error));
  }, []);

  const onSpeechResults = useCallback((e: SpeechResultsEvent) => {
    if (e.value) {
      console.log('onSpeechResults: ', e.value[0] || '');
    }
    seResults(e.value || []);
  }, []);

  const onSpeechPartialResults = useCallback((e: SpeechResultsEvent) => {
    if (e.value) {
      console.log('onSpeechPartialResults: ', e.value[0] || '');
    }
    setPartialResults(e.value || []);
  }, []);

  const onSpeechVolumeChanged = useCallback((e: any) => {
    // console.log('onSpeechVolumeChanged: ', e);
    setPitch(e.value);
  }, []);

  const registerListeners = useCallback(() => {
    Voice.onSpeechStart = onSpeechStart;
    Voice.onSpeechRecognized = onSpeechRecognized;
    Voice.onSpeechEnd = onSpeechEnd;
    Voice.onSpeechError = onSpeechError;
    Voice.onSpeechResults = onSpeechResults;
    Voice.onSpeechPartialResults = onSpeechPartialResults;
    Voice.onSpeechVolumeChanged = onSpeechVolumeChanged;
  }, [
    onSpeechEnd,
    onSpeechError,
    onSpeechPartialResults,
    onSpeechRecognized,
    onSpeechResults,
    onSpeechStart,
    onSpeechVolumeChanged,
  ]);

  const removeListeners = useCallback(async () => {
    Voice.removeAllListeners();
  }, []);

  const startRecognizing = useCallback(async () => {
    try {
      clearState();
      registerListeners();
      await Voice.start(locale);
    } catch (e) {
      console.error(e);
    }
  }, [clearState, registerListeners]);

  const stopRecognizing = useCallback(async () => {
    try {
      removeListeners();
      await Voice.stop();
      clearState();
    } catch (e) {
      console.error(e);
    }
  }, [clearState, removeListeners]);

  const cancelRecognizing = useCallback(async () => {
    try {
      await Voice.cancel();
      clearState();
    } catch (e) {
      console.error(e);
    }
  }, [clearState]);

  const destroyRecognizer = useCallback(async () => {
    try {
      await Voice.destroy();
      clearState();
    } catch (e) {
      console.error(e);
    }
  }, [clearState]);

  return (
    <SafeAreaView>
      <Text style={styles.welcome}>Welcome to React Native Voice!</Text>
      <Text style={styles.instructions}>
        Press the button and start speaking.
      </Text>
      <Text style={styles.stat}>{`Started: ${started}`}</Text>
      <Text style={styles.stat}>{`Recognized: ${recognized}`}</Text>
      <Text style={styles.stat}>{`Pitch: ${pitch}`}</Text>
      <Text style={styles.stat}>{`Error: ${error}`}</Text>
      <Text style={styles.stat}>Results</Text>
      {(results || []).map((result, index) => {
        return (
          <Text key={`result-${index}`} style={styles.stat}>
            {result}
          </Text>
        );
      })}
      <Text style={styles.stat}>Partial Results</Text>
      {(partialResults || []).map((result, index) => {
        return (
          <Text key={`partial-result-${index}`} style={styles.stat}>
            {result}
          </Text>
        );
      })}
      <Text style={styles.stat}>{`End: ${end}`}</Text>
      <View style={styles.startAndStopContainer}>
        <TouchableHighlight onPress={startRecognizing}>
          <Image style={styles.button} source={require('./button.png')} />
        </TouchableHighlight>
        <TouchableHighlight onPress={stopRecognizing}>
          <Text style={styles.action}>Stop Recognizing</Text>
        </TouchableHighlight>
        <TouchableHighlight onPress={cancelRecognizing}>
          <Text style={styles.action}>Cancel</Text>
        </TouchableHighlight>
        <TouchableHighlight onPress={destroyRecognizer}>
          <Text style={styles.action}>Destroy</Text>
        </TouchableHighlight>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  button: {
    width: 50,
    height: 50,
  },
  container: {
    display: 'flex',
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
  },
  welcome: {
    fontSize: 20,
    textAlign: 'center',
    margin: 10,
    color: 'white',
  },
  action: {
    textAlign: 'center',
    color: 'white',
    marginVertical: 5,
    fontWeight: 'bold',
    fontSize: 18,
  },
  instructions: {
    textAlign: 'center',
    color: '#333333',
    marginBottom: 5,
  },
  stat: {
    textAlign: 'center',
    color: '#B0171F',
    marginBottom: 1,
  },
  startAndStopContainer: {
    display: 'flex',
    alignItems: 'center',
    paddingVertical: 5,
    borderWidth: 1,
    backgroundColor: '#ccc',
  },
});

export default App;
