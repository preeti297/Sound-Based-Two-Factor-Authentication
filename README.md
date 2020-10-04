# Sound-Based-Two-Factor-Authentication
Sound Based Two Factor Authentication is a novel method in which sound is used as the second form of authentication, without user interference. It is a sub-set of multi-factor authentication.

The implementation is divided into three major tasks. They are:-
1. Web Application - In the Sign in page, after the first authentication is performed, the sound in surrondings is recorded.
2. Phone - A background application is used to record the sound.
3. Sound Similarity - The sound similarity is checked by calculating the correlation between the audio fingerprints.

After the sounds are match, the second authentication is successfully performed.
