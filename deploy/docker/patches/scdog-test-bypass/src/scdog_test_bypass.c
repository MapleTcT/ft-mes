/*
 * Test-environment Sentinel/SCDog shim.
 *
 * The original license service loads libSCDog through JNA during startup.
 * Linux test deployments do not have the physical software dog, so these
 * exported symbols return an authorized value and keep the normal Java call
 * chain alive without touching the business services.
 */

int CheckDogSecrityDirect(const char *key) {
    (void) key;
    return 255;
}

int DogPack_CheckDogSecurityEx(const char *key) {
    (void) key;
    return 255;
}
