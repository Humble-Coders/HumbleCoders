package com.humblecoders.humblecoders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SkillsInterestsScreen(
    onComplete: (UserProfile) -> Unit,
    userViewModel: UserViewModel? = null
) {
    var selectedSkills by remember { mutableStateOf(setOf<String>()) }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }
    var selectedCourseCategory by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf("") }
    var learningGoals by remember { mutableStateOf("") }

    val skills = listOf(
        "Programming", "Design", "Data Analysis", "Problem Solving",
        "Communication", "Project Management", "Teamwork", "Creativity",
        "Critical Thinking", "Time Management", "Leadership", "Research"
    )

    val interests = listOf(
        "Mobile Apps", "Web Development", "Data Science", "AI/ML",
        "Game Development", "Cybersecurity", "Cloud Computing", "DevOps",
        "UI/UX Design", "Blockchain", "IoT", "AR/VR"
    )

    val courseCategories = listOf("App Dev", "Web Dev", "Python")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FA),
                        Color(0xFFE9ECEF)
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            // Header with gradient background
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF4263A6),
                                    Color(0xFF5B73C4)
                                )
                            )
                        )
                        .padding(32.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Tell us about yourself",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Help us create your personalized learning journey",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        item {
            // Skills Section
            PrettySection(
                title = "Your Skills",
                subtitle = "Select skills you already have",
                icon = Icons.Outlined.Psychology,
                gradientColors = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A))
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(skills) { skill ->
                        PrettySkillChip(
                            skill = skill,
                            isSelected = selectedSkills.contains(skill),
                            onClick = {
                                selectedSkills = if (selectedSkills.contains(skill)) {
                                    selectedSkills - skill
                                } else {
                                    selectedSkills + skill
                                }
                            },
                            selectedColor = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }

        item {
            // Interests Section
            PrettySection(
                title = "Your Interests",
                subtitle = "What excites you to learn?",
                icon = Icons.Outlined.Palette,
                gradientColors = listOf(Color(0xFFFF5722), Color(0xFFFF7043))
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(interests) { interest ->
                        PrettySkillChip(
                            skill = interest,
                            isSelected = selectedInterests.contains(interest),
                            onClick = {
                                selectedInterests = if (selectedInterests.contains(interest)) {
                                    selectedInterests - interest
                                } else {
                                    selectedInterests + interest
                                }
                            },
                            selectedColor = Color(0xFFFF5722)
                        )
                    }
                }
            }
        }

        item {
            // Course Category Section
            PrettySection(
                title = "Preferred Course Category",
                subtitle = "Choose your main focus area",
                icon = Icons.Outlined.Code,
                gradientColors = listOf(Color(0xFF2196F3), Color(0xFF42A5F5))
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    courseCategories.forEach { category ->
                        PrettyCategoryCard(
                            category = category,
                            isSelected = selectedCourseCategory == category,
                            onClick = { selectedCourseCategory = category },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            // Experience Level
            PrettySection(
                title = "Experience Level",
                subtitle = "How would you rate your current skills?",
                icon = Icons.Default.Star,
                gradientColors = listOf(Color(0xFF9C27B0), Color(0xFFBA68C8))
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Beginner", "Intermediate", "Advanced").forEach { level ->
                        PrettyExperienceChip(
                            level = level,
                            isSelected = experienceLevel == level,
                            onClick = { experienceLevel = level },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            // Learning Goals
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Learning Goals",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Share what you want to achieve",
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = learningGoals,
                        onValueChange = { learningGoals = it },
                        placeholder = {
                            Text(
                                "I want to build mobile apps, learn new technologies, advance my career...",
                                color = Color.Gray.copy(alpha = 0.7f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6,
                        shape = RoundedCornerShape(16.dp)

                    )
                }
            }
        }

        item {
            // Complete Button with gradient
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Button(
                    onClick = {
                        if (selectedCourseCategory.isNotEmpty() && experienceLevel.isNotEmpty()) {
                            val userProfile = UserProfile(
                                skills = selectedSkills.toList(),
                                interests = selectedInterests.toList(),
                                preferredCourseCategory = selectedCourseCategory,
                                experienceLevel = experienceLevel,
                                learningGoals = learningGoals,
                                isOnboardingComplete = true
                            )

                            userViewModel?.markOnboardingComplete()
                            onComplete(userProfile)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    enabled = selectedCourseCategory.isNotEmpty() && experienceLevel.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color(0xFFE0E0E0)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (selectedCourseCategory.isNotEmpty() && experienceLevel.isNotEmpty()) {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF4263A6),
                                            Color(0xFF5B73C4)
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFE0E0E0),
                                            Color(0xFFE0E0E0)
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Complete Profile",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Complete",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PrettySection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            brush = Brush.linearGradient(gradientColors),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
fun PrettySkillChip(
    skill: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color
) {
    Card(
        modifier = Modifier
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(25.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) selectedColor else Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = skill,
                color = if (isSelected) Color.White else Color(0xFF2C3E50),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun PrettyCategoryCard(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (primaryColor, backgroundColor) = when (category) {
        "App Dev" -> Color(0xFF2196F3) to Color(0xFFE3F2FD)
        "Web Dev" -> Color(0xFFFF5722) to Color(0xFFFDE7E7)
        "Python" -> Color(0xFFF39C12) to Color(0xFFFEF5E7)
        else -> Color(0xFF95A5A6) to Color(0xFFF8F9FA)
    }

    Card(
        modifier = modifier
            .aspectRatio(1.2f)
            .shadow(
                elevation = if (isSelected) 12.dp else 4.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) primaryColor else backgroundColor
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = category,
                    color = if (isSelected) Color.White else primaryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PrettyExperienceChip(
    level: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF9C27B0) else Color.White
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = level,
                color = if (isSelected) Color.White else Color(0xFF9C27B0),
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
            )
        }
    }
}