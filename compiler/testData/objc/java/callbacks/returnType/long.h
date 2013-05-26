#import <Foundation/NSObject.h>

typedef long long (*fun_t)();

@interface A : NSObject
+ (void) checkResultAndPrintOK: (fun_t) fun;
@end
