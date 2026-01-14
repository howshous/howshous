package io.github.howshous.ui.screens.main_tenant

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.howshous.ui.components.DebouncedIconButton
import io.github.howshous.ui.theme.SurfaceLight

@Composable
fun EmergencyScreen(nav: NavController) {
    val context = LocalContext.current
    var showInstructions by remember { mutableStateOf(false) }
    var selectedInstruction by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceLight)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFF5252))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DebouncedIconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text(
                "Emergency",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
        }

        if (!showInstructions) {
            // Main options
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Call Emergency Services
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:911")
                            }
                            context.startActivity(intent)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF5252)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🚨",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Call Emergency Services",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "911",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                }

                // Emergency Instructions
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showInstructions = true },
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "📖",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Emergency Instructions",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Learn life-saving techniques",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Instructions list
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showInstructions = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    Text("Emergency Instructions", style = MaterialTheme.typography.titleLarge)
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(emergencyInstructions) { instruction ->
                        InstructionCard(
                            title = instruction.title,
                            onClick = { selectedInstruction = instruction.content }
                        )
                    }
                }
            }
        }
    }

    // Instruction detail dialog
    selectedInstruction?.let { content ->
        InstructionDialog(
            content = content,
            onDismiss = { selectedInstruction = null }
        )
    }
}

data class EmergencyInstruction(
    val title: String,
    val content: String
)

val emergencyInstructions = listOf(
    EmergencyInstruction(
        title = "CPR (Cardiopulmonary Resuscitation)",
        content = """
            HOW TO PERFORM CPR:
            
            1. CHECK RESPONSIVENESS
               - Tap the person's shoulder and shout "Are you okay?"
               - If no response, call 911 immediately
            
            2. CHECK BREATHING
               - Look for chest movement
               - Listen for breathing sounds
               - Feel for breath on your cheek
            
            3. START CHEST COMPRESSIONS
               - Place heel of one hand on center of chest
               - Place other hand on top, interlock fingers
               - Position yourself with shoulders over hands
               - Push hard and fast: 100-120 compressions per minute
               - Depth: 2-2.4 inches (5-6 cm)
               - Allow chest to fully recoil between compressions
            
            4. GIVE RESCUE BREATHS (if trained)
               - After 30 compressions, tilt head back, lift chin
               - Pinch nose, give 2 breaths (1 second each)
               - Watch for chest rise
            
            5. CONTINUE
               - Repeat cycles of 30 compressions and 2 breaths
               - Continue until help arrives or person shows signs of life
            
            NOTE: Hands-only CPR (compressions only) is also effective if you're not trained in rescue breathing.
        """.trimIndent()
    ),
    EmergencyInstruction(
        title = "How to Put Out a Gas Fire",
        content = """
            HOW TO PUT OUT A GAS FIRE:
            
            1. TURN OFF THE GAS SUPPLY
               - Locate the gas shut-off valve
               - Turn it clockwise to close
               - Do this ONLY if safe to do so
            
            2. DO NOT USE WATER
               - Water can spread the fire
               - Water can cause gas to explode
            
            3. USE APPROPRIATE EXTINGUISHER
               - Use Class B fire extinguisher (for flammable liquids/gases)
               - Aim at the base of the fire
               - Use PASS technique: Pull, Aim, Squeeze, Sweep
            
            4. SMOTHER THE FIRE (if no extinguisher)
               - Use a fire blanket or wet cloth
               - Cover the fire completely
               - Do not remove until fire is completely out
            
            5. EVACUATE IMMEDIATELY
               - If fire spreads or cannot be controlled
               - Get everyone out of the building
               - Call 911 from a safe location
            
            6. VENTILATE AFTER
               - Once fire is out, open windows/doors
               - Let gas dissipate before re-entering
            
            WARNING: If you cannot safely turn off the gas or extinguish the fire, evacuate immediately and call 911.
        """.trimIndent()
    ),
    EmergencyInstruction(
        title = "Choking - Heimlich Maneuver",
        content = """
            HEIMLICH MANEUVER FOR CHOKING:
            
            FOR ADULTS AND CHILDREN (over 1 year):
            
            1. RECOGNIZE CHOKING
               - Person cannot speak or breathe
               - Clutching throat (universal sign)
               - Face turning blue
            
            2. STAND BEHIND THE PERSON
               - Place one foot between their feet
               - Wrap arms around their waist
            
            3. PLACE HANDS
               - Make a fist with one hand
               - Place thumb side against upper abdomen (below ribcage)
               - Grasp fist with other hand
            
            4. PERFORM ABDOMINAL THRUSTS
               - Press inward and upward forcefully
               - Repeat until object is expelled or person becomes unconscious
               - Use quick, upward thrusts
            
            5. IF PERSON BECOMES UNCONSCIOUS
               - Lower to ground carefully
               - Begin CPR
               - Check mouth for object between compressions
            
            FOR INFANTS (under 1 year):
            - Hold face down on your forearm
            - Support head and neck
            - Give 5 back blows between shoulder blades
            - Turn over, give 5 chest thrusts
            - Repeat until object is expelled
            
            Always call 911 if choking persists or person becomes unconscious.
        """.trimIndent()
    ),
    EmergencyInstruction(
        title = "Bleeding Control",
        content = """
            HOW TO CONTROL BLEEDING:
            
            1. APPLY DIRECT PRESSURE
               - Use clean cloth, gauze, or your hand
               - Press firmly on the wound
               - Maintain pressure continuously
            
            2. ELEVATE THE INJURY
               - Raise injured area above heart level
               - Helps reduce blood flow to the area
            
            3. ADD MORE LAYERS (if needed)
               - Do not remove first layer
               - Add more cloth/gauze on top
               - Continue applying pressure
            
            4. USE PRESSURE POINTS (if severe)
               - Apply pressure to artery above wound
               - For arms: brachial artery (inside upper arm)
               - For legs: femoral artery (groin area)
            
            5. DO NOT REMOVE OBJECTS
               - If object is embedded, do not remove
               - Apply pressure around the object
               - Stabilize object with bandages
            
            6. WHEN TO CALL 911
               - Bleeding doesn't stop after 10 minutes
               - Blood is spurting (arterial bleeding)
               - Person shows signs of shock
               - Wound is large or deep
            
            NOTE: Tourniquets should only be used as a last resort for life-threatening bleeding.
        """.trimIndent()
    ),
    EmergencyInstruction(
        title = "Fire Safety - Evacuation",
        content = """
            FIRE EVACUATION PROCEDURES:
            
            1. ALERT OTHERS
               - Yell "FIRE!" to alert everyone
               - Activate fire alarm if available
            
            2. GET OUT IMMEDIATELY
               - Do not stop to collect belongings
               - Do not use elevators
               - Use stairs only
            
            3. CHECK DOORS BEFORE OPENING
               - Feel door with back of hand
               - If hot, do not open - use alternate route
               - If cool, open slowly and proceed
            
            4. STAY LOW
               - Smoke rises, stay close to floor
               - Crawl if necessary
               - Cover nose and mouth with cloth if possible
            
            5. IF TRAPPED
               - Close all doors between you and fire
               - Stuff cracks with wet cloths
               - Signal for help from window
               - Call 911 and give your location
            
            6. MEETING POINT
               - Go to designated meeting area
               - Account for all occupants
               - Do not re-enter building
            
            7. IF CLOTHES CATCH FIRE
               - STOP, DROP, and ROLL
               - Cover face with hands
               - Roll back and forth until fire is out
            
            Remember: Your safety is the priority. Get out first, then call 911.
        """.trimIndent()
    )
)

@Composable
fun InstructionCard(
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                "View",
                modifier = Modifier.rotate(180f)
            )
        }
    }
}

@Composable
fun InstructionDialog(
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency Instructions") },
        text = {
            LazyColumn {
                item {
                    Text(
                        content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = Modifier.fillMaxWidth(0.9f)
    )
}

