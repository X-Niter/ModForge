import { Layout } from "@/components/ui/layout";
import { Separator } from "@/components/ui/separator";

export default function TermsOfService() {
  return (
    <Layout>
      <div className="container mx-auto max-w-4xl">
        <h1 className="text-3xl font-bold tracking-tight mb-2">Terms of Service</h1>
        <p className="text-muted-foreground mb-6">Last updated: May 9, 2025</p>
        <Separator className="mb-6" />

        <div className="prose prose-slate dark:prose-invert max-w-none">
          <h2>1. Introduction</h2>
          <p>
            Welcome to ModForge AI ("Platform," "we," "us," or "our"). By accessing or using our Platform, you agree to comply with and be bound by these Terms of Service ("Terms"). Please read these Terms carefully before using the Platform. If you do not agree with these Terms, please do not access or use our Platform.
          </p>

          <h2>2. Ownership and Proprietary Rights</h2>
          <p>
            This Platform and all of its contents, features, and functionality (including but not limited to all information, software, code, text, displays, images, video, and audio, and the design, selection, and arrangement thereof) are owned by X_Niter and Knoxhack, their licensors, or other providers of such material and are protected by international copyright, trademark, patent, trade secret, and other intellectual property or proprietary rights laws.
          </p>

          <h2>3. License to Use</h2>
          <p>
            Subject to these Terms, we grant you a limited, non-exclusive, non-transferable, non-sublicensable license to access and use the Platform for your personal, non-commercial use. You may not:
          </p>
          <ul>
            <li>Reproduce, distribute, modify, create derivative works of, publicly display, publicly perform, republish, download, store, or transmit any of the material on our Platform, except as permitted by these Terms.</li>
            <li>Access or use the Platform for any commercial purposes.</li>
            <li>Attempt to decompile, reverse engineer, or otherwise attempt to discover the source code of the Platform.</li>
            <li>Remove any copyright, trademark, or other proprietary notices from any portion of the Platform.</li>
            <li>Transfer the materials to another person or "mirror" the materials on any other server.</li>
          </ul>

          <h2>4. User Content and Rights</h2>
          <p>
            When you submit, upload, or otherwise make available any content to the Platform ("User Content"), you grant us a worldwide, non-exclusive, royalty-free license to use, reproduce, modify, adapt, publish, translate, distribute, and display such User Content in connection with providing and promoting the Platform.
          </p>
          <p>
            You represent and warrant that: (i) you own the User Content or have the right to use and grant us the rights and license as provided in these Terms, and (ii) the posting of your User Content on or through the Platform does not violate the privacy rights, publicity rights, copyrights, contract rights, or any other rights of any person.
          </p>

          <h2>5. Minecraft Mods and Licensing</h2>
          <p>
            The Platform enables the creation of modifications ("Mods") for the Minecraft game. All Mods created using the Platform are subject to the following licensing terms:
          </p>
          <ul>
            <li>The Platform and its AI components remain the property of X_Niter and Knoxhack.</li>
            <li>The code and content of Mods generated using the Platform are owned by the user who created them, subject to Mojang's and Microsoft's terms regarding Minecraft modifications.</li>
            <li>Users may distribute Mods created using the Platform in accordance with Mojang's and Microsoft's terms for Minecraft modifications.</li>
            <li>Users may not claim that the ModForge AI Platform itself is their creation or property.</li>
          </ul>

          <h2>6. API Usage and Limitations</h2>
          <p>
            The Platform utilizes various APIs, including those from OpenAI. Your use of the Platform is subject to:
          </p>
          <ul>
            <li>Reasonable usage limits to prevent abuse of the service.</li>
            <li>Any terms imposed by third-party API providers.</li>
            <li>Temporary or permanent suspension of access for users who attempt to circumvent usage limits or otherwise abuse the service.</li>
          </ul>

          <h2>7. Prohibited Uses</h2>
          <p>
            You agree not to use the Platform:
          </p>
          <ul>
            <li>To violate any applicable law, regulation, or legal agreement.</li>
            <li>To create Mods that contain malicious code, viruses, or other harmful components.</li>
            <li>To create Mods that infringe on the intellectual property rights of others.</li>
            <li>To create Mods that contain offensive, discriminatory, or inappropriate content.</li>
            <li>To attempt to gain unauthorized access to the Platform or its related systems or networks.</li>
            <li>To interfere with or disrupt the integrity or performance of the Platform.</li>
          </ul>

          <h2>8. Termination</h2>
          <p>
            We may terminate or suspend your access to the Platform immediately, without prior notice or liability, for any reason, including if you breach these Terms. Upon termination, your right to use the Platform will immediately cease.
          </p>

          <h2>9. Disclaimer of Warranties</h2>
          <p>
            THE PLATFORM IS PROVIDED "AS IS" AND "AS AVAILABLE" WITHOUT ANY WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED. TO THE FULLEST EXTENT PERMITTED BY LAW, WE DISCLAIM ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
          </p>

          <h2>10. Limitation of Liability</h2>
          <p>
            TO THE FULLEST EXTENT PERMITTED BY LAW, IN NO EVENT WILL WE, OUR AFFILIATES, OR THEIR LICENSORS, SERVICE PROVIDERS, EMPLOYEES, AGENTS, OFFICERS, OR DIRECTORS BE LIABLE FOR DAMAGES OF ANY KIND, UNDER ANY LEGAL THEORY, ARISING OUT OF OR IN CONNECTION WITH YOUR USE, OR INABILITY TO USE, THE PLATFORM, INCLUDING ANY DIRECT, INDIRECT, SPECIAL, INCIDENTAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES.
          </p>

          <h2>11. Indemnification</h2>
          <p>
            You agree to defend, indemnify, and hold harmless X_Niter, Knoxhack, and their respective officers, directors, employees, contractors, agents, licensors, and suppliers from and against any claims, liabilities, damages, judgments, awards, losses, costs, expenses, or fees (including reasonable attorneys' fees) arising out of or relating to your violation of these Terms or your use of the Platform.
          </p>

          <h2>12. Governing Law</h2>
          <p>
            These Terms shall be governed by and construed in accordance with the laws of the United States, without regard to its conflict of law provisions.
          </p>

          <h2>13. Changes to Terms</h2>
          <p>
            We reserve the right to modify these Terms at any time. If we make changes, we will provide notice by posting the updated Terms on the Platform and updating the date at the top of these Terms. Your continued use of the Platform after any such change constitutes your acceptance of the new Terms.
          </p>

          <h2>14. Contact Information</h2>
          <p>
            For questions about these Terms, please contact us through the Platform.
          </p>
        </div>
      </div>
    </Layout>
  );
}