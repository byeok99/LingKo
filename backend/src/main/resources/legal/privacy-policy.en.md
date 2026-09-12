# LingKo Privacy Policy

- Effective date and last updated: 2026-09-12
- Operator and privacy contact: LEE SANG BYEOK (이상벽), maplebyeok@gmail.com

This policy explains how LingKo collects, uses, shares, retains and deletes information. LingKo is operated by an individual. Contact the address above for privacy questions and requests.

## 1. Data we collect and how we use it

| Data and collection method | Use |
|---|---|
| Google or Apple account identifier, provider, and available email, name or profile image, received during sign-in | Authentication, account display and account management. Apple Hide My Email supplies a private relay address; an email or name may not be returned on subsequent sign-ins |
| Practice sentences you select, type or save | Preparing pronunciation guides, assessment and practice history |
| Voice recordings you make and explicitly authorize for upload | Pronunciation assessment; recordings can contain your voice and other personal information you speak |
| Assessment results, recognized speech, overall and available word-level scores, weak-sound information | Learning feedback and history. A syllable may have no individual score |
| Session records, hashed refresh tokens, quota and reward records | Secure sessions, usage limits and reward verification |
| Terms/privacy acknowledgement, optional marketing choice, and separate AI permission/withdrawal records including notice version and time | Recording choices and enforcing AI sharing permission |
| Operational request/error information, such as request times, IP addresses and technical diagnostics | Security, incident investigation and service reliability |
| Information you send to our contact address | Responding to enquiries and privacy requests |

We do not use recordings to identify a person, create a voiceprint or train our own AI models. Do not include sensitive information, other people's voices or personal details in recordings or custom sentences.

## 2. AI assessment: recipients, data and prior permission

Before the first recording upload for assessment, the app displays **AI assessment privacy**. This notice names the recipients, explains the data and purpose, and asks you to select **Allow sharing with Microsoft Azure**. Agreeing to general Terms or acknowledging this Policy does not grant this separate permission. Existing accounts must also make this choice.

| Recipient | What is sent | Why |
|---|---|---|
| Amazon Web Services, Inc. (AWS S3) | Your voice recording, stored under a per-account object path | Private temporary storage so LingKo's evaluation worker can retrieve it |
| Microsoft Corporation (Microsoft Azure AI Speech) | Your voice recording and the reference pronunciation derived from the practice sentence | Speech recognition and pronunciation assessment, returning scores and recognized speech |
| Replicate, Inc. | LingKo's reference guide images, via image URLs, and interpolation parameters | Generating visual pronunciation guides; this workflow does not send your voice, account name/email, or your sentence as a text field |

LingKo does not attach your account name or email to Azure assessment requests. However, the audio or practice text itself may contain personal information.

For pronunciation assessment, Microsoft documents that it does not retain or store submitted audio or reference text. This describes Azure's assessment processing, not LingKo's storage of audio and results. See [Microsoft Speech data privacy](https://learn.microsoft.com/en-us/azure/ai-foundry/responsible-ai/speech-service/speech-to-text/data-privacy-security).

**Your choice:** selecting **Not now — do not send**, or closing the notice, does not upload the recording. Sentence browsing and guides remain available. Assessment requires permission. Manage or withdraw permission at **Profile → AI assessment privacy → Withdraw permission**. The server checks permission before issuing upload access, accepting a job and starting external assessment. Withdrawal prevents new assessments and stops queued work at the next permission check. A transmission already started cannot be recalled. Granting permission again does not authorize an old queued job.

Withdrawal does not delete existing results. Use **Profile → Delete account** to delete your stored account data, results and audio, or contact us for assistance.

## 3. Other third parties and protection

Google LLC and Apple Inc. handle their own account services under their privacy policies. Google LLC (AdMob) also processes advertising and technical data as described below. We do not sell your personal data.

We require any processor receiving personal data to provide the **same or an equal level of protection** as this Policy: processing limited to the service purpose, appropriate confidentiality and security, restricted access, and deletion/retention safeguards. Our use of cloud processors is subject to their applicable data-processing and service terms, including the [Microsoft Products and Services DPA](https://www.microsoft.com/licensing/docs/view/Microsoft-Products-and-Services-Data-Protection-Addendum-DPA) and [AWS Data Processing Addendum](https://aws.amazon.com/compliance/data-processing-addendum/). A new recipient or material change to AI data or purposes requires an updated notice and renewed permission before sharing.

The operator is in South Korea. AWS storage and Azure Speech are configured for Seoul (ap-northeast-2) and Korea Central respectively. Replicate and Google are US-based; provider operations may involve other countries under their service terms. Transmission occurs over HTTPS when you use the corresponding feature. You may decline AI transmission, control advertising choices, or contact us about cross-border processing. Declining AI processing prevents assessment but not browsing or guides.

## 4. Retention and deletion

| Data | Retention/deletion |
|---|---|
| Uploaded audio | Deleted after assessment completes or finally fails, including a terminal consent rejection. Unsubmitted uploads or failed individual deletions expire under the audio bucket's one-day lifecycle policy. Lifecycle deletion is asynchronous, not a guarantee of deletion at exactly 24 hours |
| Local recording | Kept temporarily on the device for recording/retry; deleted after successful assessment or when the recording is discarded. Declining sharing does not upload it |
| Account, saved sentences, assessment history, quota and consent records | Retained to provide the account and evidence choices until account deletion, subject to any applicable legal preservation requirement. There is no automatic dormant-account deletion schedule |
| Completed/failed evaluation job metadata | Normally cleaned up after seven days; this is separate from learning history |
| Operational logs and support correspondence | Kept only as needed for security, incident handling and resolving the enquiry, then deleted; contact us for an applicable retention request |
| Ad-provider records | Governed by Google's retention policies; LingKo does not separately store an advertising identifier |

Account deletion first removes your stored audio objects, then deletes related sessions, jobs, results, quota, saved sentences, consent records and account data. If storage deletion fails, the app reports failure and lets you retry instead of claiming completion. Shared guide assets do not identify you and are not deleted with an account.

## 5. Advertising and optional marketing

Google AdMob may process IP address, device/app information, ad impressions/interactions, and advertising identifiers when permitted by the device and consent settings, for ad delivery, measurement and fraud prevention. Google describes its practices in its [Privacy Policy](https://policies.google.com/privacy).

Where required, the advertising consent flow presents the available choices before ads are requested. Advertising permission is separate from AI assessment permission. You can manage device tracking/advertising settings in iOS Settings → Privacy & Security → Tracking, or Android's advertising privacy settings. Declining tracking does not prevent all technical processing needed to deliver ads.

An optional marketing choice at sign-up is separate from required acknowledgements and AI permission. Declining does not restrict service access. To withdraw a marketing choice, contact maplebyeok@gmail.com or use an unsubscribe link in a message, if provided. Necessary service/security responses are not marketing.

## 6. Security and your rights

We use HTTPS, authenticated API access, private audio storage with short-lived upload access, input/ownership validation and restricted access to secrets. Refresh tokens are stored as hashes on the server. Device credentials use secure storage. Do not assume uninstalling alone deletes server data or all iOS Keychain entries; use Sign out or Delete account.

You can request access, correction, erasure or restriction of processing by contacting maplebyeok@gmail.com. You can read the Terms and Policy in Profile and withdraw AI permission there. We respond without undue delay in accordance with applicable law, verify account ownership when necessary, and explain any legally required retention. Data incidents are handled and notified as required by applicable law.

Additional rights depend on your jurisdiction. Korean users may also contact the Personal Information Dispute Mediation Committee (kopico.go.kr) or KISA's privacy reporting service (privacy.kisa.or.kr). This service is intended for users aged 16 or over; if we learn that a younger person has provided personal data, contact us so we can arrange deletion.

## 7. Automated feedback and changes

Scores are automated language-learning feedback, not a medical assessment or a decision about eligibility for employment or services. They may be inaccurate. Contact us with concerns about a result.

Material policy changes are communicated through the app or an appropriate service notice. Changes to AI recipients, data or purpose require renewed permission before transmission.

Revision: 2026-09-12 — Separate AI permission, named recipients and transfer details, withdrawal, and retention corrections.

[한국어](./privacy-policy.ko.md) · [Terms of Service](./terms-of-service.en.md)
