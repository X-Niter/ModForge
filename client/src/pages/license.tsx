import { Separator } from "@/components/ui/separator";

export default function LicensePage() {
  return (
    <div className="container mx-auto max-w-4xl">
      <h1 className="text-3xl font-bold tracking-tight mb-2">License Agreement</h1>
      <p className="text-muted-foreground mb-6">Last updated: May 9, 2025</p>
      <Separator className="mb-6" />

      <div className="prose prose-slate dark:prose-invert max-w-none">
        <h2>MODFORGE AI PROPRIETARY SOFTWARE LICENSE</h2>
        
        <h3>1. OWNERSHIP AND COPYRIGHT</h3>
        <p>
          ModForge AI and all of its components, code, algorithms, and functionality (collectively, the "Software") 
          are the exclusive proprietary property of X_Niter and Knoxhack (the "Owners"). The Software is protected 
          by copyright laws and international copyright treaties, as well as other intellectual property laws and treaties.
        </p>

        <h3>2. LICENSE GRANT</h3>
        <p>
          Subject to the terms and conditions of this Agreement, the Owners hereby grant you a limited, 
          non-exclusive, non-transferable, non-sublicensable license to access and use the Software 
          for your personal use only. All rights not expressly granted to you are reserved by the Owners.
        </p>

        <h3>3. RESTRICTIONS</h3>
        <p>
          Except as expressly permitted by this Agreement or by applicable law, you may not:
        </p>
        <ol type="a" className="list-lower-alpha">
          <li>Copy, modify, adapt, or create derivative works based on the Software;</li>
          <li>Reverse engineer, decompile, disassemble, or otherwise attempt to discover the source code or underlying algorithms of the Software;</li>
          <li>Rent, lease, sublicense, sell, assign, distribute, or otherwise transfer rights to the Software;</li>
          <li>Remove, alter, or obscure any proprietary notices on the Software;</li>
          <li>Use the Software to develop competing products or services;</li>
          <li>Use the Software for any commercial purpose without explicit written permission from the Owners;</li>
          <li>Attempt to circumvent any usage limitations or security measures in the Software;</li>
          <li>Allow third parties to access or use the Software unless explicitly permitted by this Agreement;</li>
          <li>Use the Software in any manner that violates applicable laws or regulations.</li>
        </ol>

        <h3>4. OUTPUT OWNERSHIP</h3>
        <p>
          The ownership of content created using the Software ("Output") is subject to the following conditions:
        </p>
        <ol type="a" className="list-lower-alpha">
          <li>Minecraft mods created using the Software may be used, shared, and distributed by you, subject to Mojang's and Microsoft's terms and conditions regarding Minecraft modifications;</li>
          <li>You may not claim that the Software itself or its underlying technology is your creation or property;</li>
          <li>You retain ownership of your original content that you input into the Software;</li>
          <li>The Owners retain all rights to the technology, methods, and systems used to generate the Output.</li>
        </ol>

        <h3>5. TERMINATION</h3>
        <p>
          This license is effective until terminated. The Owners may terminate this license at any time if you fail to comply 
          with any term of this Agreement. Upon termination, you must cease all use of the Software and destroy all copies 
          of the Software in your possession or control.
        </p>

        <h3>6. DISCLAIMER OF WARRANTIES</h3>
        <p>
          THE SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING, 
          BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, 
          OR NON-INFRINGEMENT. THE OWNERS DO NOT WARRANT THAT THE SOFTWARE WILL MEET YOUR REQUIREMENTS 
          OR THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE.
        </p>

        <h3>7. LIMITATION OF LIABILITY</h3>
        <p>
          IN NO EVENT SHALL THE OWNERS BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
          DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
          DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
          WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY 
          OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
        </p>

        <h3>8. EXPORT COMPLIANCE</h3>
        <p>
          You agree to comply with all applicable laws and regulations regarding export and re-export of the Software 
          or any direct product thereof.
        </p>

        <h3>9. GOVERNING LAW</h3>
        <p>
          This Agreement shall be governed by and construed in accordance with the laws of the United States, 
          without giving effect to any principles of conflicts of law.
        </p>

        <h3>10. ENTIRE AGREEMENT</h3>
        <p>
          This Agreement constitutes the entire agreement between you and the Owners regarding the subject matter hereof 
          and supersedes all prior or contemporaneous oral or written agreements concerning such subject matter.
        </p>

        <h3>11. RESERVATION OF RIGHTS</h3>
        <p>
          All rights not expressly granted in this Agreement are reserved by the Owners. Nothing in this Agreement 
          shall limit the Owners' right to develop, use, license, create derivative works of, or otherwise exploit 
          the Software, or to permit third parties to do so.
        </p>

        <h3>12. ACKNOWLEDGMENT</h3>
        <p>
          BY USING THE SOFTWARE, YOU ACKNOWLEDGE THAT YOU HAVE READ THIS AGREEMENT, UNDERSTAND IT, AND AGREE TO BE BOUND 
          BY ITS TERMS AND CONDITIONS. IF YOU DO NOT AGREE TO THE TERMS OF THIS AGREEMENT, DO NOT USE THE SOFTWARE.
        </p>

        <div className="border-t border-border/40 mt-8 pt-6">
          <p className="font-semibold">Copyright © 2025 X_Niter and Knoxhack. All Rights Reserved.</p>
        </div>
      </div>
    </div>
  );
}