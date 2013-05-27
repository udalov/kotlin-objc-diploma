#import <Foundation/NSObject.h>

typedef void (*fun_t)(double);

@interface A : NSObject
+ (void) invokeWithPi: (fun_t) fun;
@end
