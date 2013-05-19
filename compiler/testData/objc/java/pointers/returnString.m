#import "returnString.h"

const char *hello = "Hello";

@implementation A
+ (const char *) getHello {
    return hello;
}

+ (NSString *) getOK {
    return [[NSString alloc] initWithString:@"OK"];
}
@end

