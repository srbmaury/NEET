package com.neet.app.ui.reference

// Hand-authored, not OpenAI-generated — unlike topic Notes, this is cross-cutting toolkit
// material (constants, trig, vectors, calculus) rather than syllabus content, and precision
// matters enough here (a wrong digit in a constant actively misleads) that it isn't worth the
// hallucination risk an LLM call would carry. No backend/network involvement at all.
val quickReferenceMarkdown = """
**Physical Constants**

- Speed of light in vacuum: §§c = 3 \times 10^8 \text{ m/s}§§
- Planck's constant: §§h = 6.63 \times 10^{-34} \text{ J s}§§
- Elementary charge: §§e = 1.6 \times 10^{-19} \text{ C}§§
- Electron mass: §§m_e = 9.1 \times 10^{-31} \text{ kg}§§
- Proton mass: §§m_p = 1.673 \times 10^{-27} \text{ kg}§§
- Neutron mass: §§m_n = 1.675 \times 10^{-27} \text{ kg}§§
- Avogadro's number: §§N_A = 6.022 \times 10^{23} \text{ mol}^{-1}§§
- Universal gas constant: §§R = 8.314 \text{ J mol}^{-1}\text{K}^{-1} = 0.0821 \text{ L atm mol}^{-1}\text{K}^{-1}§§
- Boltzmann constant: §§k_B = 1.38 \times 10^{-23} \text{ J/K}§§
- Faraday constant: §§F = 96500 \text{ C/mol}§§
- Gravitational constant: §§G = 6.674 \times 10^{-11} \text{ N m}^2\text{/kg}^2§§
- Acceleration due to gravity: §§g = 9.8 \text{ m/s}^2§§ — commonly rounded to 10 m/s² for NEET numericals.
- Permittivity of free space: §§\epsilon_0 = 8.85 \times 10^{-12} \text{ C}^2\text{N}^{-1}\text{m}^{-2}§§
- Permeability of free space: §§\mu_0 = 4\pi \times 10^{-7} \text{ T m/A}§§
- Coulomb's constant: §§k = \frac{1}{4\pi\epsilon_0} \approx 9 \times 10^9 \text{ N m}^2\text{C}^{-2}§§
- Stefan-Boltzmann constant: §§\sigma = 5.67 \times 10^{-8} \text{ W m}^{-2}\text{K}^{-4}§§
- Atomic mass unit: §§1u = 1.66 \times 10^{-27} \text{ kg} = 931.5 \text{ MeV}/c^2§§
- Speed of sound in air (20°C): ≈ 343 m/s (often taken as 340 m/s).
- Molar volume of an ideal gas at STP: 22.4 L/mol.

**SI Prefixes**

- femto (f) = §§10^{-15}§§, pico (p) = §§10^{-12}§§, nano (n) = §§10^{-9}§§, micro (µ) = §§10^{-6}§§, milli (m) = §§10^{-3}§§
- centi (c) = §§10^{-2}§§, kilo (k) = §§10^{3}§§, mega (M) = §§10^{6}§§, giga (G) = §§10^{9}§§

**Unit Conversions**

- §§1 \text{ eV} = 1.6 \times 10^{-19} \text{ J}§§
- §§1 \text{ atm} = 1.013 \times 10^5 \text{ Pa}§§
- §§K = °C + 273.15§§
- §§1 \text{ cal} = 4.184 \text{ J}§§

**Trigonometry — Standard Angles**

- 0°: sin = 0, cos = 1, tan = 0
- 30°: sin = 1/2, cos = √3/2, tan = 1/√3
- 45°: sin = 1/√2, cos = 1/√2, tan = 1
- 60°: sin = √3/2, cos = 1/2, tan = √3
- 90°: sin = 1, cos = 0, tan = undefined
- 180°: sin = 0, cos = −1, tan = 0
- 270°: sin = −1, cos = 0, tan = undefined
- 360°: sin = 0, cos = 1, tan = 0

**Trigonometry — Identities**

- §§\sin^2\theta + \cos^2\theta = 1§§
- §§1 + \tan^2\theta = \sec^2\theta§§
- §§1 + \cot^2\theta = \csc^2\theta§§
- §§\sin(A \pm B) = \sin A \cos B \pm \cos A \sin B§§
- §§\cos(A \pm B) = \cos A \cos B \mp \sin A \sin B§§
- §§\sin 2\theta = 2 \sin\theta \cos\theta§§
- §§\cos 2\theta = \cos^2\theta - \sin^2\theta = 2\cos^2\theta - 1 = 1 - 2\sin^2\theta§§
- Small-angle approximation (θ in radians, θ small): §§\sin\theta \approx \theta§§, §§\tan\theta \approx \theta§§, §§\cos\theta \approx 1 - \frac{\theta^2}{2}§§

**Vectors**

- Dot product (scalar result): §§\vec{A} \cdot \vec{B} = |\vec{A}||\vec{B}|\cos\theta = A_xB_x + A_yB_y + A_zB_z§§
- Cross product (vector result, right-hand rule for direction): §§\vec{A} \times \vec{B} = |\vec{A}||\vec{B}|\sin\theta \, \hat{n}§§ — magnitude equals the area of the parallelogram spanned by A and B.
- Magnitude: §§|\vec{A}| = \sqrt{A_x^2 + A_y^2 + A_z^2}§§
- Unit vector along A: §§\hat{A} = \vec{A}/|\vec{A}|§§
- Component form: §§\vec{A} = A_x\hat{i} + A_y\hat{j} + A_z\hat{k}§§

**Basic Calculus**

- §§\frac{d}{dx}(x^n) = nx^{n-1}§§
- §§\frac{d}{dx}(\sin x) = \cos x§§, §§\frac{d}{dx}(\cos x) = -\sin x§§
- §§\frac{d}{dx}(e^x) = e^x§§, §§\frac{d}{dx}(\ln x) = \frac{1}{x}§§
- §§\int x^n \, dx = \frac{x^{n+1}}{n+1} + C§§ (n ≠ −1)
- §§\int \sin x \, dx = -\cos x + C§§, §§\int \cos x \, dx = \sin x + C§§
- §§\int e^x \, dx = e^x + C§§
- Physics usage: §§v = \frac{dx}{dt}§§, §§a = \frac{dv}{dt} = \frac{d^2x}{dt^2}§§, §§x = \int v \, dt§§

**Approximations & Algebra**

- Binomial approximation, for §§|x| \ll 1§§: §§(1+x)^n \approx 1 + nx§§
- Quadratic formula: §§x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}§§
- §§\log(ab) = \log a + \log b§§, §§\log(a/b) = \log a - \log b§§, §§\log(a^n) = n \log a§§
- For small x: §§\ln(1+x) \approx x§§
""".trimIndent().replace('§', '$')
