package com.hxozzi.hyopago

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.hxozzi.hyopago.ui.theme.HyopagoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HyopagoTheme {
                MainScreen()
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    HyopagoTheme {
        MainScreen()
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current

    //room
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    var textInput by remember { mutableStateOf("") }
    var newText by remember { mutableStateOf("") } // 0대신 빈값으로 변경

    var expanded by remember { mutableStateOf(false) } // dropdown
    var isReady by remember { mutableStateOf(false) }  // isready 변수 선언

    val kochTranslator = remember {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.KOREAN) // 입력어
            .setTargetLanguage(TranslateLanguage.CHINESE)  // 번역어
            .build()
        Translation.getClient(options)
    }

    // 1. 음성을 텍스트로 - STT
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }

    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    recognizedText = "오류 발생: $error"
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    recognizedText = matches?.get(0).orEmpty()
                    textInput = recognizedText // 음성 인식을 입력 필드에 반영

                    // 번역 호출
                    val kochTranslator = Translation.getClient(
                        TranslatorOptions.Builder()
                            .setSourceLanguage(TranslateLanguage.KOREAN)
                            .setTargetLanguage(TranslateLanguage.CHINESE)
                            .build()
                    )
                    kochTranslator.translate(recognizedText)
                        .addOnSuccessListener { translatedText ->
                            newText = translatedText
                        }
                        .addOnFailureListener {
                            newText = "번역 오류 발생"
                        }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    val speechRecognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR") // 한국어로 설정
        }
    }

    // 2. 텍스트를 음성으로 - TTS
    // TTS 초기화
    val textToSpeech = remember {
        var tts: TextToSpeech? = null
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setLanguage(Locale.CHINESE)?.let { result ->
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "중국어 TTS를 지원하지 않습니다.")
                    }
                }
            } else {
                Log.e("TTS", "TTS 초기화 실패")
            }
        }.also { tts = it }
        tts
    }

    // 음성 출력 함수
    fun speakText(text: String) {
        if (text.isNotEmpty() && textToSpeech != null) {
            textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                null
            )
        } else {
            Log.e("TTS", "TTS가 초기화되지 않았거나 출력할 텍스트가 없습니다.")
        }
    }


    // 3. 갤러리에서 이미지 불러온 후 텍스트로 변환
    var text by remember { mutableStateOf("") }
    var recognaizedText by remember { mutableStateOf("") }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia())
    { uri ->
        if (uri != null) {
            val recognizer =
                TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            val image: InputImage
            try {
                image = InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        textInput = visionText.text //textInput으로 변경

                        // 번역 추가
                        kochTranslator.translate(textInput)
                            .addOnSuccessListener { translatedText ->
                                newText = translatedText // 번역된 텍스트 저장
                            }
                            .addOnFailureListener { exception ->
                                newText = "번역 오류 발생" // 실패 시 메시지
                            }
                    }
            } catch (e: Exception) { // 예외 처리
            }
        }
    }

    LaunchedEffect(kochTranslator) {
        var conditions = DownloadConditions.Builder() // 사용 될 떄만 변수 사용
            .build()
        kochTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                isReady = true // 준비가 되었을 때
            }
    }


    Column( // 제목
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "HYOPAGO",
                style = TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB2D8F0),
                )
            )
            Text(
                "孝巴哥",
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = Color(0xFF89CFF0),
                )
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        // 중간
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image( // 즐겨찾기
                painter = painterResource(id = R.drawable.study),
                contentDescription = "즐겨찾기",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .clickable {
                        val intent = Intent(context, STARActivity::class.java)
                        context.startActivity(intent)
                    }
            )
            Text(
                "즐겨찾기",
                fontSize = 15.sp
            )
//            Spacer(modifier = Modifier.width(5.dp))
//            // 즐겨찾기 버튼
//            Icon(
//                imageVector = Icons.Default.Add,
//                contentDescription = "즐겨찾기",
//                modifier = Modifier
//                    .clickable {
//                        scope.launch(Dispatchers.IO) {
//                            db
//                                .saveDataDao()
//                                .insertAll(
//                                    SaveData(input = textInput, result = newText)
//                                )
//                        }
//                        Toast
//                            .makeText(context, "즐겨찾기에 추가되었습니다.", Toast.LENGTH_SHORT)
//                            .show()
//                    }
//            )
        }


        // 번역창 부분
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 1. 번역할 내용 입력 필드
            OutlinedTextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    // 입력 시 번역 호출
                    kochTranslator.translate(it)
                        .addOnSuccessListener { translatedText ->
                            newText = translatedText // 번역된 텍스트 저장
                        }
                        .addOnFailureListener { exception ->
                            newText = "번역 오류 발생" // 실패 시 메시지
                        }
                },
                label = { Text("번역할 내용을 입력하세요") },
                modifier = Modifier
                    .width(350.dp)
                    .height(200.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Blue,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.width(10.dp))

            // 2. 번역된 내용 출력 필드
            OutlinedTextField(
                value = newText,
                onValueChange = {},
                label = { Text("번역 결과") },
                readOnly = true,
                modifier = Modifier
                    .width(350.dp)
                    .height(200.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Red,
                    unfocusedBorderColor = Color.Gray
                )
            )
        }
        Spacer(modifier = Modifier.padding(bottom = 20.dp))

        // 1. 음성 인식 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(color = Color(0xFFF0A0A0))
                .clickable { // 버튼 역할을 수행
                    if (!isListening) {
                        isListening = true
                        speechRecognizer.startListening(speechRecognizerIntent)
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = "음성 인식",
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(1.dp)) // 아이콘과 텍스트 사이 간격 추가
            Text(
                "음성 인식",
                color = Color.White
            )
        }

        // 2. 음성 출력 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(color = Color(0xFFFFE4B5))
                .clickable {
                    // TTS로 번역된 텍스트 출력
                    speakText(newText) // newText에 저장된 번역 결과 출력
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "음성 출력",
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(1.dp)) // 아이콘과 텍스트 사이 간격 추가
            Text(
                "음성 출력",
                color = Color.White
            )
        }

        // 3. 이미지 버튼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(color = Color(0xFFB0E0B2))
                .clickable {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = "이미지 인식",
                tint = Color.White
            )
            Text(
                "이미지 인식",
                color = Color.White
            )
        }

        // 4. 즐겨찾기에 추가
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(color = Color(0xFFB0E0E6))
                .clickable {
                    scope.launch(Dispatchers.IO) {
                        db
                            .saveDataDao()
                            .insertAll(
                                SaveData(input = textInput, result = newText)
                            )
                    }
                    Toast
                        .makeText(context, "즐겨찾기에 추가되었습니다.", Toast.LENGTH_SHORT)
                        .show()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "즐겨찾기",
                tint = Color.White
            )
            Text(
                "즐겨찾기 추가",
                color = Color.White
            )
        }

    }
}