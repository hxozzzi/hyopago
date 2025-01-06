package com.hxozzi.hyopago

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.hxozzi.hyopago.ui.theme.HyopagoTheme

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
    val language = listOf("한국어","베트남어") // 언어 리스트 준비
    var textInput by remember { mutableStateOf("") }
    var newText by remember { mutableStateOf("") } // 0대신 빈값으로 변경

    val kovnTranslator = remember {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.KOREAN) // 입력어
            .setTargetLanguage(TranslateLanguage.VIETNAMESE)  // 번역어
            .build()
        Translation.getClient(options)
    }

    var isReady by remember { mutableStateOf(false) }  // isready 변수 선언

    LaunchedEffect(kovnTranslator) {
        var conditions = DownloadConditions.Builder() // 사용 될 떄만 변수 사용
            .build()
        kovnTranslator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                isReady = true // 준비가 되었을 때
            }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("번역할 내용을 입력하세요") },
            modifier = Modifier
                .width(280.dp)
                .height(100.dp)

        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedButton(
            onClick = {
                kovnTranslator.translate(textInput)
                    .addOnSuccessListener { translatedText ->
                        newText = translatedText // 번역 된 값 출력
                    }
                    .addOnFailureListener { exception ->
                    }            },
            enabled = isReady, // 번역할 준비가 되면 버튼이 만들어짐
        ) {
            Text("번역")
        }
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = newText,
            onValueChange = {},
            label = { Text("번역") },
            readOnly = true, // 읽기 전용으로 설정
            modifier = Modifier
                .width(280.dp)
                .height(100.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Red,
                unfocusedBorderColor = Color.Red
            )
        )
        Spacer(modifier = Modifier.height(20.dp))
        Image(
            painter = painterResource(id = R.drawable.memo),
            contentDescription = "메모",
            modifier = Modifier
                .wrapContentSize()
                .clickable {
                    navcontroller.navigate()
                }
        )
    }
}