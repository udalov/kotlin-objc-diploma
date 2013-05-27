#import <Foundation/NSObject.h>

typedef void (*fun_t)(char, char, char);

@interface A : NSObject
+ (void) invokeWithABC: (fun_t) fun;
@end
