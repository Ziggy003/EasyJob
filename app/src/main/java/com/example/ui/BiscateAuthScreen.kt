package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Laranja

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiscateAuthScreen(
    viewModel: BiscateViewModel,
    onAccessAsGuest: () -> Unit
) {
    val context = LocalContext.current
    var isSignUp by remember { mutableStateOf(false) }

    // COMMON FORM FIELDS
    var name by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var selectedBairro by remember { mutableStateOf("Talatona") }

    // ROLE SELECT: "CLIENTE" or "PRESTADOR"
    var selectedRole by remember { mutableStateOf("CLIENTE") }

    // PRESTADOR ONLY SPECIFIC FIELDS
    var selectedSpecialty by remember { mutableStateOf("Pintura") }
    var experience by remember { mutableStateOf("") }
    var competencies by remember { mutableStateOf("") }
    var workEvidenceStr by remember { mutableStateOf("") }

    val bairros = listOf("Talatona", "Kilamba", "Viana", "Cazenga", "Sambizanga", "Maianga", "Cacuaco", "Samba")
    val specialties = listOf("Pintura", "Limpeza", "Electricidade", "Construção", "Canalização", "Carpintaria", "Outros")

    var isBairroExpanded by remember { mutableStateOf(false) }
    var isSpecialtyExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Branding logo and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "BISCATE",
                    fontWeight = FontWeight.ExtraBold,
                    color = Laranja,
                    letterSpacing = 2.sp,
                    fontSize = 32.sp
                )
                Box(
                    modifier = Modifier
                        .background(Laranja, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "AO",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }

            Text(
                text = "Honestidade, Sem Algoritmos, Sem Taxas Ocultas",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Dynamic card for state switching (Login vs Signup)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Segmented selection tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isSignUp) Laranja else Color.Transparent)
                                .clickable { isSignUp = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Entrar",
                                fontWeight = FontWeight.Bold,
                                color = if (!isSignUp) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSignUp) Laranja else Color.Transparent)
                                .clickable { isSignUp = true }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Registar",
                                fontWeight = FontWeight.Bold,
                                color = if (isSignUp) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (!isSignUp) {
                        // LOGIN METHOD
                        Text(
                            text = "Aceder à Plataforma",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        OutlinedTextField(
                            value = contact,
                            onValueChange = { contact = it },
                            label = { Text("Número de Telemóvel") },
                            placeholder = { Text("Ex: +244 931 445 221") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().testTag("login_phone_input")
                        )

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("PIN de Acesso (6 dígitos)") },
                            visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { pinVisible = !pinVisible }) {
                                    Icon(
                                        imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Esconder/Ver"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth().testTag("login_pin_input")
                        )

                        Button(
                            onClick = {
                                if (contact.isBlank() || pin.isBlank()) {
                                    Toast.makeText(context, "Por favor, preencha as credenciais.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.login(
                                        contact = contact,
                                        pin = pin,
                                        onSuccess = {
                                            Toast.makeText(context, "Sessão iniciada com sucesso! Bem-vindo de volta.", Toast.LENGTH_SHORT).show()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Laranja),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .testTag("login_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Entrar", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                        }

                        // Helpful login assistance card containing standard seeds
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Laranja, modifier = Modifier.size(16.dp))
                                    Text("Contas de Demonstração (Seeded):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Laranja)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "• Cliente: +244 931 445 221 (PIN: 123456)\n• Pintor (Manuel): +244 923 111 001 (PIN de teste: 123456)\n• Limpezas (Rosa): +244 923 222 002 (PIN de teste: 123456)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                )
                            }
                        }

                    } else {
                        // SIGN UP (REGISTRAR) METHOD
                        Text(
                            text = "Criar Nova Conta",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Role Selector toggle
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedRole == "CLIENTE") Laranja.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (selectedRole == "CLIENTE") Laranja else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedRole = "CLIENTE" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (selectedRole == "CLIENTE") Laranja else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Quero Contratar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (selectedRole == "CLIENTE") Laranja else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Sou Cliente",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedRole == "PRESTADOR") Laranja.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (selectedRole == "PRESTADOR") Laranja else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedRole = "PRESTADOR" }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Engineering,
                                        contentDescription = null,
                                        tint = if (selectedRole == "PRESTADOR") Laranja else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Quero Trabalhar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (selectedRole == "PRESTADOR") Laranja else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Sou Prestador",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome Completo") },
                            leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().testTag("reg_name_input")
                        )

                        OutlinedTextField(
                            value = contact,
                            onValueChange = { contact = it },
                            label = { Text("Contacto de Telemóvel (+244)") },
                            placeholder = { Text("9XXXXXXXX") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth().testTag("reg_phone_input")
                        )

                        OutlinedTextField(
                            value = pin,
                            onValueChange = { pin = it },
                            label = { Text("Criar PIN Secreto de Acesso (6 digitos)") },
                            visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth().testTag("reg_pin_input")
                        )

                        // BAIRRO DROPDOWN
                        Box(modifier = Modifier.fillMaxWidth()) {
                            ExposedDropdownMenuBox(
                                expanded = isBairroExpanded,
                                onExpandedChange = { isBairroExpanded = !isBairroExpanded }
                            ) {
                                OutlinedTextField(
                                    value = "Bairro de Atuação: $selectedBairro",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBairroExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = isBairroExpanded,
                                    onDismissRequest = { isBairroExpanded = false }
                                ) {
                                    bairros.forEach { b ->
                                        DropdownMenuItem(
                                            text = { Text(b) },
                                            onClick = {
                                                selectedBairro = b
                                                isBairroExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // DYNAMIC PRESTADOR SECTION
                        if (selectedRole == "PRESTADOR") {
                            Text(
                                text = "Dados Profissionais do Prestador",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Laranja,
                                modifier = Modifier.padding(top = 6.dp)
                            )

                            // SPECIALTY DROPDOWN
                            Box(modifier = Modifier.fillMaxWidth()) {
                                ExposedDropdownMenuBox(
                                    expanded = isSpecialtyExpanded,
                                    onExpandedChange = { isSpecialtyExpanded = !isSpecialtyExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = "Especialidade: $selectedSpecialty",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSpecialtyExpanded) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = isSpecialtyExpanded,
                                        onDismissRequest = { isSpecialtyExpanded = false }
                                    ) {
                                        specialties.forEach { spec ->
                                            DropdownMenuItem(
                                                text = { Text(spec) },
                                                onClick = {
                                                    selectedSpecialty = spec
                                                    isSpecialtyExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = experience,
                                onValueChange = { experience = it },
                                label = { Text("Anos & Descrição de Experiência") },
                                placeholder = { Text("Ex: Trabalho com pintura residencial desde 2018 em Angola. Fiz acabamentos de cimento queimado na centralidade...") },
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth().testTag("reg_experience_input")
                            )

                            OutlinedTextField(
                                value = competencies,
                                onValueChange = { competencies = it },
                                label = { Text("Suas Competências Chave") },
                                placeholder = { Text("Ex: Isolamento perfeito de móveis, Pintor certificado, Pontualidade rígida") },
                                minLines = 1,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth().testTag("reg_competence_input")
                            )

                            OutlinedTextField(
                                value = workEvidenceStr,
                                onValueChange = { workEvidenceStr = it },
                                label = { Text("Trabalhos feitos / Portfólio (Separados por Barra '|')") },
                                placeholder = { Text("Ex: Pintura interna T4 no Kilamba|Mural texturado em pavilhão comercial") },
                                minLines = 1,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth().testTag("reg_portfolio_input")
                            )
                        }

                        Button(
                            onClick = {
                                if (name.isBlank() || contact.isBlank() || pin.isBlank()) {
                                    Toast.makeText(context, "Preencha todos os campos fundamentais.", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.register(
                                        name = name,
                                        contact = contact,
                                        pin = pin,
                                        bairro = selectedBairro,
                                        role = selectedRole,
                                        category = selectedSpecialty,
                                        experiencia = experience,
                                        competencias = competencies,
                                        provasTrabalho = workEvidenceStr,
                                        onSuccess = {
                                            Toast.makeText(context, "Sua conta de $selectedRole foi criada e ativada!", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Laranja),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .testTag("register_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Criar Conta & Entrar", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Guest Mode Bypasser Label
            TextButton(
                onClick = onAccessAsGuest,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Aceder temporariamente como Visitante ➔",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
