#import "nsstring.h"

@implementation A
+ (NSString *) getString {
    return [[NSString alloc] initWithString:@"Hello"];
}
@end
