#import "noFinalizeInJava.h"

A *instance;

@implementation A
+ (void) initialize {
    [super initialize];
    instance = [[A alloc] init];
}

+ (A *) get {
    return instance;
}
@end
